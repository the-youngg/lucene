package com.sendroids.lucene.search.service;

import com.sendroids.lucene.config.BipsProperties;
import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.search.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.Similarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
class MySearch {
    private final Analyzer myAnalyzer;
    private final Similarity mySimilarity;
    private final MyField myField;
    private final MyQueryParser myQueryParser;
    private final LuceneConfig luceneConfig;
    @Autowired
    private final BipsProperties properties;
    private final ApplicationContext applicationContext;

    @Autowired
    public MySearch(Analyzer myAnalyzer, Similarity mySimilarity, MyField myField, MyQueryParser myQueryParser, LuceneConfig luceneConfig, BipsProperties properties, ApplicationContext applicationContext) {
        this.myAnalyzer = myAnalyzer;
        this.mySimilarity = mySimilarity;
        this.myField = myField;
        this.myQueryParser = myQueryParser;
        this.luceneConfig = luceneConfig;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    public Collection<Result> search(
            final String keyword,
            final int maxNumber,
            final Project.ProjectType projectType
    ) {
        if (keyword.isEmpty()) {
            return new ArrayList<>();
        }
        List<IndexSearcher> indexSearchers = SearchHelper.getAllProjectIndexSearcher(projectType, properties);
        Query query = myQueryParser.searchParser(keyword, myAnalyzer);
//        log.info("Lucene QueryParser : " + query);
        Collection<Result> collection = new ArrayList<>();

        indexSearchers.forEach(
                indexSearcher -> {
                    collection.addAll(search(indexSearcher, myAnalyzer, query, maxNumber).join());
                }
        );

        return collection.stream()
                .sorted(Comparator.comparing(Result::getScore).reversed())
                .limit(maxNumber)
                .collect(Collectors.toList());
    }

    public List<Project> findIndexProject(String field, String key, int maxNum) {
        List<Project> projects = new ArrayList<>();
        List<IndexSearcher> indexSearcherList = SearchHelper.getAllProjectIndexSearcher(Project.ProjectType.PROVISION, properties);
        indexSearcherList.addAll(SearchHelper.getAllProjectIndexSearcher(Project.ProjectType.CONSUMPTION, properties));
        indexSearcherList.forEach(
                indexSearcher -> {
                    try {
                        projects.addAll(findAndAddProjects(projects, field, key, maxNum, indexSearcher));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return projects.stream()
                .limit(maxNum)
                .collect(Collectors.toList());
    }

    public List<Document> findIndexDocument(String field, String key, int maxNum) {
        //this cost time
        List<Document> documents = new ArrayList<>();
        List<IndexSearcher> indexSearcherList = SearchHelper.getAllProjectIndexSearcher(Project.ProjectType.PROVISION, properties);
        indexSearcherList.addAll(SearchHelper.getAllProjectIndexSearcher(Project.ProjectType.CONSUMPTION, properties));
        indexSearcherList.forEach(
                indexSearcher -> {
                    try {
                        documents.addAll(findAndAddDocuments(documents, field, key, maxNum, indexSearcher));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        return documents.stream()
                .limit(maxNum)
                .collect(Collectors.toList());
    }

    public List<Document> findIndexDocument(IndexSearcher indexSearcher, String field, String key, int maxNum) {
        List<Document> documents = new ArrayList<>();
        try {
            documents = findAndAddDocuments(documents, field, key, maxNum, indexSearcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return documents;
    }

    @Async
    public CompletableFuture<Collection<Result>> search(IndexSearcher indexSearcher, Analyzer analyzer, Query query, int num) {
        Collection<Result> collection = new ArrayList<>();
        try {
            TopDocs topDocs = indexSearcher.search(query, num);
            //topDocs = ranking(topDocs);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            if (scoreDocs.length > 0) {
                int fragmenterSize = LuceneConfig.LUCENE_MAX_FRAGMENTER_SIZE;
                for (ScoreDoc scoreDoc : scoreDocs) {

                    Document document = indexSearcher.doc(scoreDoc.doc);
                    if (Optional.ofNullable(document).isPresent()
                            && !StringUtils.isEmpty(document.get(myField.projectId))) {
                        Result result = new Result();

                        Highlighter highlighter = getHighlighter(query);
                        String kw = "";
                        String brief = "";
                        if (!StringUtils.isEmpty(document.get(myField.keyword))) {
                            kw = highlighter.getBestFragment(analyzer, myField.keyword, document.get(myField.keyword));
                            kw = ifNotMatchHighlightOther(document, kw, fragmenterSize, myField.keyword);
                        }
                        if (!StringUtils.isEmpty(document.get(myField.brief))) {
                            brief = highlighter.getBestFragment(analyzer, myField.brief, document.get(myField.brief));
                            brief = ifNotMatchHighlightOther(document, brief, fragmenterSize, myField.brief);
                        }
                        //todo 1019 get match word

                        result.setProjectId(Long.parseLong(document.get(myField.projectId)));
                        result.setProjectName(document.get(myField.projectName) == null ? "" : document.get(myField.projectName));
                        result.setKeyword(kw);
                        result.setBrief(brief);
                        result.setScore(scoreDoc.score);
                        collection.add(result);
                    } else if (StringUtils.isEmpty(document.get(myField.projectId))) {
                        log.warn("Invalid null project Id! Lucene QueryParser: " + query + ", match Project #" + document.get(myField.projectId) +
                                ", score:" + scoreDoc.score); //  + ", Score detail:" + indexSearcher.explain(query, scoreDoc.doc));
                    }

                }
//                log.info("Results:" + collection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(collection);
    }

    private Highlighter getHighlighter(Query query) {
        QueryTermScorer source = new QueryTermScorer(query);
        Formatter formatter = new SimpleHTMLFormatter(LuceneConfig.LUCENE_HIGHLIGHT_PRE_TAG, LuceneConfig.LUCENE_HIGHLIGHT_POST_TAG);
        Fragmenter fragmenterLength = new SimpleFragmenter(LuceneConfig.LUCENE_MAX_FRAGMENTER_SIZE);

        Highlighter highlighter = new Highlighter(formatter, source);
        highlighter.setTextFragmenter(fragmenterLength);

        return highlighter;
    }

    private String ifNotMatchHighlightOther(Document document, String kw, int fragmenterSize, String field) {
        if (StringUtils.isEmpty(kw)) {
            kw = document.get(field);
            kw = kw.substring(0, kw.length() < fragmenterSize ? kw.length() : fragmenterSize);
        }
        return kw;
    }

    private List<Project> findAndAddProjects(List<Project> projects, String field, String key, int maxNum, IndexSearcher indexSearcher) throws IOException {
        Query query = new TermQuery(new Term(field, key));
        TopDocs topDocs = indexSearcher.search(query, maxNum);
        if (topDocs.totalHits > 0) {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = indexSearcher.doc(scoreDoc.doc);
                Project project = new Project();

                project.setId(Long.parseLong(document.get(myField.getProjectId())));
                project.setName(document.get(myField.getProjectName()));
                project.setType(Project.ProjectType.valueOf(document.get(myField.getType())));

                projects.add(project);
            }
        }
        return projects;
    }

    private List<Document> findAndAddDocuments(List<Document> documents, String field, String key, int maxNum, IndexSearcher indexSearcher) throws IOException {
        Query query = new TermQuery(new Term(field, key));
        TopDocs topDocs = indexSearcher.search(query, maxNum);
        if (topDocs.totalHits > 0) {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                documents.add(indexSearcher.doc(scoreDoc.doc));
            }
        }
        return documents;
    }

    public Collection<Result> removeTheSameProjectInResult(Collection<Result> list1, Collection<Result> list2) {
        List<Result> afterList1 = removeSameProject(list1);
        List<Result> afterList2 = removeSameProject(list2);
        afterList1.removeIf(
                iResult -> {
                    final boolean[] existed = {false};
                    afterList2.forEach(
                            pResult -> {
                                if (pResult.getProjectId().equals(iResult.getProjectId())) {
                                    pResult.setScore(iResult.getScore() + pResult.getScore());
                                    existed[0] = true;
                                }
                            }
                    );
                    return existed[0];
                }
        );
        afterList2.addAll(afterList1);
        return afterList2;
    }

    private List<Result> removeSameProject(Collection<Result> resultList) {
        List<Result> afterList = resultList.stream()
                .sorted(Comparator.comparing(Result::getProjectId))
                .collect(Collectors.toList());
        for (int i = 0; i < afterList.size() - 1; i++) {
            if (afterList.get(i).getProjectId().equals(afterList.get(i + 1).getProjectId())) {
                if (afterList.get(i).getScore() < afterList.get(i + 1).getScore()) {
                    afterList.remove(i);
                } else {
                    afterList.remove(i + 1);
                }
                i--;
            }
        }
        return afterList;
    }

    private TopDocs ranking(TopDocs topDocs) {
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (int i = 0; i < scoreDocs.length; i++) {
//            scoreDocs[i]
        }
        // TODO add ranking
        return new TopDocs(topDocs.totalHits, scoreDocs, topDocs.getMaxScore());
    }

}