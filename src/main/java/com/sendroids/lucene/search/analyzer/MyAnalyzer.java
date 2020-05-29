package com.sendroids.lucene.search.analyzer;

import com.google.common.collect.Sets;
import jdk.internal.util.xml.impl.ReaderUTF8;
import lombok.NoArgsConstructor;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class MyAnalyzer extends StopwordAnalyzerBase {
    private final Mode mode;
    private final Set<String> stoptags;
    private final UserDictionary userDict;

    public MyAnalyzer() {
        this(null, JapaneseTokenizer.DEFAULT_MODE, MyAnalyzer.DefaultSetHolder.DEFAULT_STOP_SET, MyAnalyzer.DefaultSetHolder.DEFAULT_STOP_TAGS);
    }

    private MyAnalyzer(UserDictionary userDict, Mode mode, CharArraySet stopwords, Set<String> stoptags) {
        super(stopwords);
        if (userDict == null) {
            try {
                String userDictFile = "userdict.txt";
                userDict = UserDictionary.open(new ReaderUTF8(new FileInputStream(new File(userDictFile))));
            } catch (IOException e) {
                userDict = null;
            }
        }
        this.userDict = userDict;
        this.mode = mode;
        this.stoptags = stoptags;
    }

    public static CharArraySet getDefaultStopSet() {
        return DefaultSetHolder.DEFAULT_STOP_SET;
    }

    public static Set<String> getMyKeywordSet() {
        return DefaultSetHolder.MY_KEYWORD_SET;
    }

    public static Set<String> getDefaultStopTags() {
        return DefaultSetHolder.DEFAULT_STOP_TAGS;
    }

    protected TokenStreamComponents createComponents(String fieldName) {
        //jp
        JapaneseTokenizer source = new JapaneseTokenizer(this.userDict, true, this.mode);
        JapaneseBaseFormFilter stream = new JapaneseBaseFormFilter(source);
        JapanesePartOfSpeechStopFilter stream1 = new JapanesePartOfSpeechStopFilter(stream, this.stoptags);
        CJKWidthFilter stream2 = new CJKWidthFilter(stream1);
        StopFilter stream3 = new StopFilter(stream2, this.stopwords);
        JapaneseKatakanaStemFilter stream4 = new JapaneseKatakanaStemFilter(stream3);
        TokenStream stream5 = mySynonyms(stream4);
        LowerCaseFilter stream6 = new LowerCaseFilter(stream5);
        PorterStemFilter stream7 = new PorterStemFilter(stream6);

       //en
//        StandardTokenizer source = new StandardTokenizer();
//        StandardFilter result = new StandardFilter(source);
//        EnglishPossessiveFilter result2 = new EnglishPossessiveFilter(result);
//        LowerCaseFilter result3 = new LowerCaseFilter(result2);
//        StopFilter stream3 = new StopFilter(result3, this.stopwords);
//        TokenStream stream5 = mySynonyms(stream3);
//        PorterStemFilter stream7 = new PorterStemFilter((TokenStream)stream5);

        return new TokenStreamComponents(source, stream7);
    }

    protected TokenStream normalize(String fieldName, TokenStream in) {
//        SynonymGraphFilter result = new SynonymGraphFilter(in,getSynonymMap(),true);
        TokenStream result = mySynonyms(in);
        result = new CJKWidthFilter(result);
        result = new LowerCaseFilter(result);

        return result;
    }

    private static class DefaultSetHolder {
        static final CharArraySet DEFAULT_STOP_SET;
        static final Set<String> DEFAULT_STOP_TAGS;
        static final Set<String> MY_KEYWORD_SET;

        private DefaultSetHolder() {
        }

        static {
            try {
                DEFAULT_STOP_SET = loadStopwordSet(true, MyAnalyzer.class, "stop_words.txt", "#");
                CharArraySet ex = loadStopwordSet(false, MyAnalyzer.class, "stop_tags.txt", "#");
                CharArraySet keywords = MyAnalyzer.loadStopwordSet(false, MyAnalyzer.class, "weight_words.txt", "#");
                DEFAULT_STOP_TAGS = new HashSet();

                for (Object element : ex) {
                    char[] chars = (char[]) element;
                    DEFAULT_STOP_TAGS.add(new String(chars));
                }

                MY_KEYWORD_SET = new HashSet();
                Iterator var2 = keywords.iterator();
                StringBuilder sb = new StringBuilder();
                while (var2.hasNext()) {
                    Object element = var2.next();
                    char[] chars = (char[]) element;
                    sb.append(new String(chars) + " ");
                }
                String afterAnalyse = MyQueryUtil.analyseToken(sb.toString(), "", new MyAnalyzer());
                String[] sp = afterAnalyse.split(" ");
                MY_KEYWORD_SET.addAll(Sets.newHashSet(sp));

            } catch (IOException var4) {
                throw new RuntimeException("Unable to load default stopword or stoptag set");
            }
        }
    }

    /**
     * 同义词
     *
     * @param tokenizer
     * @return
     */
    public TokenStream mySynonyms(TokenStream tokenizer) {
        Map<String, String> filterargs = new HashMap<>();
        filterargs.put("ignoreCase", "true");
        filterargs.put("synonyms", "synonyms.txt");
        filterargs.put("format", "solr");
        filterargs.put("expand", "true");
//        filterargs.put("analyzer", "MyAnalyzer");
//        filterargs.put("tokenizerFactory", "solr.SynonymGraphFilterFactory");
        SynonymGraphFilterFactory factory = new SynonymGraphFilterFactory(filterargs);

        try {
            factory.inform(new ClasspathResourceLoader(MyAnalyzer.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return factory.create(tokenizer);
    }

}
