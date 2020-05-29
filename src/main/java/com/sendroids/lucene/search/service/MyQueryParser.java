package com.sendroids.lucene.search.service;


import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.search.analyzer.MyQueryUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * deal with the input
 * set weight
 */
@Component
public class MyQueryParser {
    private final MyField myField;


    @Autowired
    public MyQueryParser(MyField myField) {
        this.myField = myField;
    }

    public Query searchParser(String keyword, Analyzer analyzer) {
        Query query1 = normalParser(keyword, analyzer, myField.keyword);
        Query query2 = normalParser(keyword, analyzer, myField.brief);
        Query query3 = normalParser(keyword, analyzer, "name");
        Query query4 = normalParser(keyword, analyzer, myField.content);
        //set weight
        BoostQuery boostQuery1 = new BoostQuery(query1, LuceneConfig.LUCENE_SEARCH_KEYWORD_WEIGHT);
        BoostQuery boostQuery2 = new BoostQuery(query2, LuceneConfig.LUCENE_SEARCH_BRIEF_WEIGHT);
        BoostQuery boostQuery9 = new BoostQuery(query3, 11.0f);
        BoostQuery boostQuery10 = new BoostQuery(query4, 11.0f);
        //OCCUR : MUST SHOULD MUST_NOT
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(boostQuery1, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery2, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery9, BooleanClause.Occur.SHOULD);
        builder.add(boostQuery10, BooleanClause.Occur.SHOULD);

        return builder.build();
    }


    private Query normalParser(String keyword, Analyzer analyzer, String field) {
        String keywordEscape = MyQueryUtil.escape(keyword);
        try {
            QueryParser parser = new QueryParser(field, analyzer);
            return parser.parse(keywordEscape);
        } catch (ParseException e) {
            return new TermQuery(new Term(field, keywordEscape));
        }
    }

}
