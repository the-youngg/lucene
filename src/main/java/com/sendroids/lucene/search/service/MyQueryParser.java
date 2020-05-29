package com.sendroids.lucene.search.service;


import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.search.analyzer.MyAnalyzer;
import com.sendroids.lucene.search.analyzer.MyQueryUtil;
import com.sendroids.lucene.service.ProjectService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;


/**
 * deal with the input
 * set weight
 */
@Component
public class MyQueryParser {
    private final LuceneConfig luceneConfig;
    private final MyField myField;
    private final ProjectService projectService;


    @Autowired
    public MyQueryParser(LuceneConfig luceneConfig, MyField myField, ProjectService projectService) {
        this.luceneConfig = luceneConfig;
        this.myField = myField;
        this.projectService = projectService;
    }

    public Query searchParser(String keyword, Analyzer analyzer) {
        Query query1 = normalParser(keyword, analyzer, myField.keyword);
        Query query2 = normalParser(keyword, analyzer, myField.brief);
        Query query3 = normalParser(keyword, analyzer, myField.projectField);
        Query query4 = normalParser(keyword, analyzer, myField.projectCategory);
        Query query5 = normalParser(keyword, analyzer, myField.projectPartner);
        Query query6 = normalParser(keyword, analyzer, myField.projectScope);
        Query query7 = normalParser(keyword, analyzer, myField.projectPhase);
        Query query8 = normalParser(keyword, analyzer, myField.projectIP);
        Query query9 = normalParser(keyword, analyzer, "name");
        //set weight
        BoostQuery boostQuery1 = new BoostQuery(query1, LuceneConfig.LUCENE_SEARCH_KEYWORD_WEIGHT);
        BoostQuery boostQuery2 = new BoostQuery(query2, LuceneConfig.LUCENE_SEARCH_BRIEF_WEIGHT);
        BoostQuery boostQuery3 = new BoostQuery(query3, LuceneConfig.LUCENE_SEARCH_FIELD_WEIGHT);
        BoostQuery boostQuery4 = new BoostQuery(query4, LuceneConfig.LUCENE_SEARCH_CATEGORY_WEIGHT);
        BoostQuery boostQuery5 = new BoostQuery(query5, LuceneConfig.LUCENE_SEARCH_PARTNERSHIP_WEIGHT);
        BoostQuery boostQuery6 = new BoostQuery(query6, LuceneConfig.LUCENE_SEARCH_SCOPE_WEIGHT);
        BoostQuery boostQuery7 = new BoostQuery(query7, LuceneConfig.LUCENE_SEARCH_PHASE_WEIGHT);
        BoostQuery boostQuery8 = new BoostQuery(query8, LuceneConfig.LUCENE_SEARCH_IP_STATUS_WEIGHT);
        BoostQuery boostQuery9 = new BoostQuery(query9, 11.0f);
        //OCCUR : MUST SHOULD MUST_NOT
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(boostQuery1, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery2, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery3, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery4, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery5, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery6, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery7, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery8, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery9, BooleanClause.Occur.SHOULD);

        return builder.build();
    }


//    private Query parser3(IndexSearcher indexSearcher, String keyword, Analyzer analyzer, String field) {
//        String keyword1 = MyQueryUtil.escape(keyword);
//        try {
//            BooleanQuery.Builder builder = new BooleanQuery.Builder();
//            Query highWeightWords = getHighWeightWords();
//            TermQuery termQuery;
//
//            QueryParser parser = new QueryParser(field, analyzer);
//            Query query = parser.parse(keyword1);
//
//            Set<Term> termSet = new HashSet<>();
//            Weight weight = query.createWeight(indexSearcher, false);
//            weight.extractTerms(termSet);
//
//            if (termSet.size() > 0) {
//                for (Term t : termSet) {
//                    termQuery = new TermQuery(t);
//                    BoostingQuery boostingQuery = new BoostingQuery(termQuery, highWeightWords, luceneConfig.getBriefWeight());
//                    builder.add(boostingQuery, BooleanClause.Occur.SHOULD);
//                }
//                BooleanQuery booleanQuery = builder.build();
//                return booleanQuery;
//            }
//        } catch (ParseException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return new TermQuery(new Term(field, keyword1));
//    }

    private Query normalParser(String keyword, Analyzer analyzer, String field) {
        String keywordEscape = MyQueryUtil.escape(keyword);
        try {
            QueryParser parser = new QueryParser(field, analyzer);
            return parser.parse(keywordEscape);
        } catch (ParseException e) {
            return new TermQuery(new Term(field, keywordEscape));
        }
    }

    /**
     * if contain keyword in the dictionary set weight
     */
    private Query parserByDict(String keyword, Analyzer analyzer, String field, Float fieldWeight) {
        String keyword1 = MyQueryUtil.escape(keyword);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        String keywordAfter = MyQueryUtil.analyseToken(keyword1, "", analyzer);
        Set<String> keywords = MyAnalyzer.getMyKeywordSet();
        keywords.forEach(
                kw -> {
                    if (keywordAfter.contains(kw)) {
                        TermQuery termQuery = new TermQuery(new Term(field, kw));
                        BoostQuery boostQuery = new BoostQuery(termQuery, fieldWeight);
                        builder.add(boostQuery, BooleanClause.Occur.SHOULD);
                    }
                }
        );
        return builder.build();
    }

    public Query standardParser(IndexSearcher indexSearcher, String keyword, Analyzer analyzer, String field) throws QueryNodeException {
        String keyword1 = MyQueryUtil.escape(keyword);
        StandardQueryParser qpHelper = new StandardQueryParser();
        qpHelper.setAllowLeadingWildcard(true);
        qpHelper.setAnalyzer(analyzer);
        Query query = qpHelper.parse(keyword1, field);
        return query;

    }

    private String getFileCotentByIndexSearcherAndPid(IndexSearcher indexSearcher, Long projectId) {
        //get file content by indexSearcher
        String fileContent = "";
        try {
            TopDocs topDocs = indexSearcher.search(new TermQuery(new Term(myField.projectId, String.valueOf(projectId))), 1);
            if (topDocs.totalHits == 1) {
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    String fileExistedInIndex = indexSearcher.doc(scoreDoc.doc).get(myField.fileContent);
                    if (fileExistedInIndex != null) {
                        fileContent = fileExistedInIndex;
                    }
                }
            }
        } catch (Exception e) {

        }
        return fileContent;
    }

    public static void main(String[] a) throws ParseException {
        String query = "hello world";
        String[] fields = {"a", "b"};
        BooleanClause.Occur[] occurs = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD};
        Query query1 = MultiFieldQueryParser.parse(query, fields, occurs, new MyAnalyzer());
        System.out.println(query1);
    }

}
