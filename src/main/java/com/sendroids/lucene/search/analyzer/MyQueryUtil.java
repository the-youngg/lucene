package com.sendroids.lucene.search.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MyQueryUtil {


    public static String escape(String keyword) {
        //stop_words will remove 'a'
        String keyword1 = "a";
        if (!keyword.trim().equals("")) {
            keyword1 = QueryParser.escape(keyword.toLowerCase());
//            keyword1 = escapeQueryChars(keyword.toLowerCase());
        }
        return keyword1;
    }

    public static boolean hasEscapeQueryChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == '&' || c == ';' || c == '/'
                    || Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }

    // Analyzer 分词情况
    public static void showToken(String source, String fieldName,Analyzer analyzer) {
        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(source));
//        BoostAttribute boostAttribute = stream.addAttribute(BoostAttribute.class);
//        BytesTermAttribute bytesTermAttribute = stream.addAttribute(BytesTermAttribute.class);
        CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
//        FlagsAttribute flagsAttribute = stream.addAttribute(FlagsAttribute.class);
//        FuzzyTermsEnum.LevenshteinAutomataAttribute levenshteinAutomataAttribute = stream.addAttribute(FuzzyTermsEnum.LevenshteinAutomataAttribute.class);
        KeywordAttribute keywordAttribute = stream.addAttribute(KeywordAttribute.class);
//        LegacyNumericTokenStream.LegacyNumericTermAttribute legacyNumericTermAttribute = stream.addAttribute(LegacyNumericTokenStream.LegacyNumericTermAttribute.class);
        TypeAttribute typeAttribute = stream.addAttribute(TypeAttribute.class);
//        TermToBytesRefAttribute termToBytesRefAttribute = stream.addAttribute(TermToBytesRefAttribute.class);
        PositionIncrementAttribute positionIncrementAttribute = stream.addAttribute(PositionIncrementAttribute.class);
        PositionLengthAttribute positionLengthAttribute = stream.addAttribute(PositionLengthAttribute.class);
        ReadingAttribute readingAttribute = stream.addAttribute(ReadingAttribute.class);
        try {

            stream.reset();
            StringBuilder stringBuilder = new StringBuilder();
            while (stream.incrementToken()) {
                stringBuilder.append("[").append(charTermAttribute).append("]");
            }
            log.info("Before Lucene Analyzer: " + source);
            log.info("After  Lucene Analyzer: " + stringBuilder.toString());
            stream.close();
        } catch (IOException e) {
            log.debug("Show Token failed..");
        }
    }

    public static String analyseToken(String source, String fieldName, Analyzer analyzer) {
        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(source));
        CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
        List<String> charTerms = new ArrayList<>();
        try {
            stream.reset();
            while (stream.incrementToken()) {
                charTerms.add(charTermAttribute.toString());
            }
            stream.close();
        } catch (IOException e) {
            log.debug("analyse Token failed..");
        }
        return String.join(" ", charTerms);
    }
}
