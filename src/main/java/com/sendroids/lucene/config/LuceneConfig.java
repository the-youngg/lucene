package com.sendroids.lucene.config;


import com.sendroids.lucene.search.analyzer.MyAnalyzer;
import com.sendroids.lucene.search.service.MySimilarity;
import lombok.Data;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.similarities.Similarity;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
@Data
public class LuceneConfig {
    public static final int LUCENE_MAX_SEARCH_MONTH = 6;
    public static final int LUCENE_MAX_SEARCH_SIZE = 1024;// 1024M
    public static final int LUCENE_MAX_MATCH_MONTH = 12;

    public static final String LUCENE_HIGHLIGHT_PRE_TAG = "<span style=\"color:red;font-weight:bold;\">";
    public static final String LUCENE_HIGHLIGHT_POST_TAG = "</span>";
    public static final int LUCENE_MAX_FRAGMENTER_SIZE = 100;

    public static final float LUCENE_SEARCH_KEYWORD_WEIGHT = 10.0f;
    public static final float LUCENE_SEARCH_BRIEF_WEIGHT = 8.0f;
    public static final float LUCENE_SEARCH_FILE_CONTENT_WEIGHT = 3.0f;
    public static final float LUCENE_SEARCH_PARTNERSHIP_WEIGHT = 15.0f;
    public static final float LUCENE_SEARCH_PHASE_WEIGHT = 5.0f;
    public static final float LUCENE_SEARCH_IP_STATUS_WEIGHT = 5.0f;
    public static final float LUCENE_SEARCH_FIELD_WEIGHT = 15.0f;
    public static final float LUCENE_SEARCH_CATEGORY_WEIGHT = 15.0f;
    public static final float LUCENE_SEARCH_SCOPE_WEIGHT = 15.0f;


    @Bean
    CommandLineRunner setQueryConfig() {
        return (args) -> {
            BooleanQuery.setMaxClauseCount(10240);
        };
    }

    @Bean
    public static Analyzer myAnalyzer() {
        return new MyAnalyzer();
    }

    @Bean
    public static Similarity similarity() {
        return new MySimilarity();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IndexWriterConfig indexWriterConfig(Analyzer myAnalyzer, Similarity similarity) {
        return new IndexWriterConfig(myAnalyzer)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
                .setRAMBufferSizeMB(16)//default 16M
                .setCommitOnClose(true)
                .setSimilarity(similarity)
                .setUseCompoundFile(false);
    }

    public static String getIndexDirDateFomat(Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyyMM");
        return "/" + formatter.format(date);
    }
}
