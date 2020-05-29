package com.sendroids.lucene.search.service;


import com.sendroids.lucene.config.BipsProperties;
import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Component
public class SearchHelper {
    public static Map<String, IndexSearcher> indexSearcherListSell = new HashMap<>();//project: /201710; file: /file/201710
    public static Map<String, IndexSearcher> indexSearcherListBuy = new HashMap<>();
    public static Map<String, IndexWriter> indexWriterMap = new HashMap<>();

    @Autowired
    private BipsProperties properties;
    private static ApplicationContext context;

    @Value("${bips.i18n.test.key}")
    private String japanKeyForTest;

    @Autowired
    public SearchHelper(ApplicationContext context) {
        SearchHelper.context = context;
    }

//    public static List<String> contents = new ArrayList<>();

    @Bean
    @Profile({"dev", "test"})
    CommandLineRunner initIndexDev(IndexService indexService, ProjectService projectService, BipsProperties properties,
                                   ApplicationContext context, MessageSource messageSource) {
        return (args) -> {
            logForI18nTest(messageSource, japanKeyForTest);
            new File(properties.getIndexLocation()).deleteOnExit();
            indexService.initIndex();
//            indexService.initIndex();
            Iterable<Project> projects = projectService.getAllProjects();
            for (Project project : projects) {
                indexService.createIndex(project);
            }
            refreshAllIndexSearcher(properties);
        };
    }

    private void logForI18nTest(MessageSource messageSource, String key) {
//        log.info("I18n test:" + LocaleContextHolder.getLocale() + " get " + messageSource.getMessage(key, null, LocaleContextHolder.getLocale()));
//        log.info("I18n test:" + Locale.US + " get " + messageSource.getMessage(key, null, Locale.US));
//        log.info("I18n test:" + Locale.ENGLISH + " get " + messageSource.getMessage(key, null, Locale.ENGLISH));
//        log.info("I18n test:" + Locale.JAPAN + " get " + messageSource.getMessage(key, null, Locale.JAPAN));
//        log.info("I18n test:" + Locale.JAPANESE + " get " + messageSource.getMessage(key, null, Locale.JAPANESE));
    }

    @Bean
    @Profile({"live"})
    CommandLineRunner initIndexLive(IndexService indexService, ProjectService projectService, BipsProperties properties,
                                    MessageSource messageSource, ApplicationContext context) {
        return (args) -> {
            logForI18nTest(messageSource, japanKeyForTest);

            File sellIndex = new File(properties.getProjectLocation());
            boolean sellHasSegment = false;
            if (sellIndex.exists() && sellIndex.isDirectory()) {
                sellHasSegment = checkHasSegment(sellIndex);
            }
            Collection<Project> projects = projectService.getAllProjects();
            indexService.initIndex();
            projects.forEach(indexService::createIndex);
            refreshAllIndexSearcher(properties);
        };
    }

    private boolean checkHasSegment(File indexDir) {
        final boolean[] hasSegment = {false};
        Optional.ofNullable(indexDir.listFiles()).ifPresent(
                files -> {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            for (File file1 : file.listFiles()) {
                                if (file1.getName().contains("segment")) {
                                    hasSegment[0] = true;
                                    break;
                                }
                            }
                        }
                    }
                }
        );
        return hasSegment[0];
    }

    public static IndexWriter getIndexWriter(String path) throws IOException {
        String key = new File(path).getPath();
        Optional<IndexWriter> indexWriterOptional = Optional.ofNullable(indexWriterMap.get(key));
        if (indexWriterOptional.isPresent()) {
            if (indexWriterOptional.get().isOpen()) {
                return indexWriterOptional.get();
            }
        }
        {
            Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(key));
            IndexWriterConfig indexWriterConfig = (IndexWriterConfig) context.getBean("indexWriterConfig");
            IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
            indexWriterMap.put(key, indexWriter);
            return indexWriterMap.get(key);
        }
    }

    private static void refreshAllIndexSearcher(BipsProperties properties) {
        //check dir
        checkDirToAddIndexSearcher(new File(properties.getProjectLocation()));
        checkDirToAddIndexSearcher(new File(properties.getFileLocation()));
        indexSearcherListBuy.forEach(
                (dir, indexSearcher) -> refreshIndexSearcherIfChanged(indexSearcher));
        indexSearcherListSell.forEach(
                (dir, indexSearcher) -> refreshIndexSearcherIfChanged(indexSearcher));
    }

    private static void checkDirToAddIndexSearcher(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            for (File indexDir : Objects.requireNonNull(dir.listFiles())) {
                IndexSearcher indexSearcher = createIndexSearcher(indexDir.getPath());
                indexSearcherListSell.put(indexDir.getPath(), indexSearcher);
            }
        }
    }

    public static IndexSearcher getIndexSearcherByIndexDirName(String indexDirName) {
        String indexPath = getMapKeyByPath(indexDirName);
        IndexSearcher indexSearcher = SearchHelper.indexSearcherListSell.get(indexPath);

        // if existed, get from indexReader
        indexSearcher = refreshIndexSearcherIfChanged(indexSearcher);
        // not existed, create a new one
        if (!Optional.ofNullable(indexSearcher).isPresent()) {
            indexSearcher = createIndexSearcher(indexPath);
            indexSearcherListSell.put(indexPath, indexSearcher);
        }
        return indexSearcher;
    }


    public static List<IndexSearcher> getAllProjectIndexSearcher(BipsProperties properties) {
//        refreshAllIndexSearcher();
        List<IndexSearcher> projectIndexSearcherList = new ArrayList<>();
        indexSearcherListSell.forEach(
                (dir, indexSearcher) -> {
                    if (isSonDirectory(dir, properties.getProjectLocation())) {
                        projectIndexSearcherList.add(refreshIndexSearcherIfChanged(indexSearcher));
                    }
                }
        );
        return projectIndexSearcherList;
    }


    private static boolean isSonDirectory(String sonDir, String faDir) {
        try {
            return Files.isSameFile(Paths.get(sonDir).getParent(), Paths.get(faDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getMapKeyByPath(String path) {
        return new File(path).getPath();
    }

    private static IndexSearcher createIndexSearcher(String indexDirName) {
        FSDirectory indexDir;
        IndexSearcher indexSearcher = null;
        try {
            indexDir = FSDirectory.open(Paths.get(indexDirName));
            if (DirectoryReader.indexExists(indexDir)) {
                final DirectoryReader directoryReader = DirectoryReader.open(indexDir);
                indexSearcher = new IndexSearcher(directoryReader);
                indexSearcher.setSimilarity(LuceneConfig.similarity());
            } else {
                IndexWriter indexWriter = getIndexWriter(getMapKeyByPath(indexDirName));
                indexWriter.commit();
                indexWriter.close();
                final DirectoryReader directoryReader = DirectoryReader.open(indexDir);
                indexSearcher = new IndexSearcher(directoryReader);
                indexSearcher.setSimilarity(LuceneConfig.similarity());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return indexSearcher;
    }

    //refresh if index change
    private static IndexSearcher refreshIndexSearcherIfChanged(IndexSearcher indexSearcher) {
        if (Optional.ofNullable(indexSearcher).isPresent()) {
            try {
                DirectoryReader oldReader2 = (DirectoryReader) indexSearcher.getIndexReader();
                if (!oldReader2.isCurrent()) {
                    DirectoryReader newReader2 = DirectoryReader.openIfChanged(oldReader2);
                    indexSearcher = new IndexSearcher(newReader2);
                    indexSearcher.setSimilarity(LuceneConfig.similarity());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return indexSearcher;
    }
}