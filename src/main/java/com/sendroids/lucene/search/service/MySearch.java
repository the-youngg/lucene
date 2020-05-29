package com.sendroids.lucene.search.service;

import com.sendroids.lucene.config.BipsProperties;
import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.search.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final MyField myField;
    private final MyQueryParser myQueryParser;
    private final BipsProperties properties;

    @Autowired
    public MySearch(
            Analyzer myAnalyzer,
            MyField myField,
            MyQueryParser myQueryParser,
            BipsProperties properties
    ) {
        this.myAnalyzer = myAnalyzer;
        this.myField = myField;
        this.myQueryParser = myQueryParser;
        this.properties = properties;
    }

    public Collection<Result> search(
            final String keyword,
            final int maxNumber
    ) {
        if (keyword.isEmpty()) {
            return new ArrayList<>();
        }
        List<IndexSearcher> indexSearchers = SearchHelper.getAllProjectIndexSearcher(properties);
        Query query = myQueryParser.searchParser(keyword, myAnalyzer);
//        log.info("Lucene QueryParser : " + query);
        Collection<Result> collection = new ArrayList<>();

        indexSearchers.forEach(
                indexSearcher -> collection.addAll(search(indexSearcher, myAnalyzer, query, maxNumber).join())
        );

        return collection.stream()
                .sorted(Comparator.comparing(Result::getScore).reversed())
                .limit(maxNumber)
                .collect(Collectors.toList());
    }


    public List<Document> findIndexDocument(IndexSearcher indexSearcher, String field, String key, int maxNum) {
        List<Document> documents = new ArrayList<>();
        try {
            findAndAddDocuments(documents, field, key, maxNum, indexSearcher);
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
                    } else {
                        assert document != null;
                        if (StringUtils.isEmpty(document.get(myField.projectId))) {
                            log.warn("Invalid null project Id! Lucene QueryParser: " + query + ", match Project #" + document.get(myField.projectId) +
                                    ", score:" + scoreDoc.score); //  + ", Score detail:" + indexSearcher.explain(query, scoreDoc.doc));
                        }
                    }

                }
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
            kw = kw.substring(0, Math.min(kw.length(), fragmenterSize));
        }
        return kw;
    }


    private void findAndAddDocuments(List<Document> documents, String field, String key, int maxNum, IndexSearcher indexSearcher) throws IOException {
        Query query = new TermQuery(new Term(field, key));
        TopDocs topDocs = indexSearcher.search(query, maxNum);
//        if (topDocs.totalHits > 0) {
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(indexSearcher.doc(scoreDoc.doc));
        }
//        }
    }


}