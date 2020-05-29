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
import org.springframework.context.i18n.LocaleContextHolder;
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
            indexService.initIndex(Project.ProjectType.CONSUMPTION);
            indexService.initIndex(Project.ProjectType.PROVISION);
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
            File buyIndex = new File(properties.getProjectBuyLocation());
            File sellIndex = new File(properties.getProjectSellLocation());
            boolean buyHasSegment = false;
            boolean sellHasSegment = false;
            if (buyIndex.exists() && sellIndex.exists() && buyIndex.isDirectory() && sellIndex.isDirectory()) {
                sellHasSegment = checkHasSegment(sellIndex);
                buyHasSegment = checkHasSegment(buyIndex);
            }
            Collection<Project> projects = projectService.getAllProjects();
            if (!buyHasSegment) {
                indexService.initIndex(Project.ProjectType.CONSUMPTION);
                projects.stream()
                        .filter(project -> project.getType().equals(Project.ProjectType.CONSUMPTION))
                        .forEach(indexService::createIndex);
            }
            if (!sellHasSegment) {
                indexService.initIndex(Project.ProjectType.PROVISION);
                projects.stream()
                        .filter(project -> project.getType().equals(Project.ProjectType.PROVISION))
                        .forEach(indexService::createIndex);
            }
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
        checkDirToAddIndexSearcher(Project.ProjectType.CONSUMPTION, new File(properties.getProjectBuyLocation()));
        checkDirToAddIndexSearcher(Project.ProjectType.PROVISION, new File(properties.getProjectSellLocation()));
        checkDirToAddIndexSearcher(Project.ProjectType.CONSUMPTION, new File(properties.getFileBuyLocation()));
        checkDirToAddIndexSearcher(Project.ProjectType.PROVISION, new File(properties.getFileSellLocation()));
        indexSearcherListBuy.forEach(
                (dir, indexSearcher) -> refreshIndexSearcherIfChanged(indexSearcher));
        indexSearcherListSell.forEach(
                (dir, indexSearcher) -> refreshIndexSearcherIfChanged(indexSearcher));
    }

    private static void checkDirToAddIndexSearcher(Project.ProjectType projectType, File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            for (File indexDir : dir.listFiles()) {
                if (indexDir.exists() && indexDir.isDirectory()) {
                    switch (projectType) {
                        case CONSUMPTION:
                            if (!Optional.ofNullable(indexSearcherListBuy.get(indexDir.getPath())).isPresent()) {
                                IndexSearcher indexSearcher = createIndexSearcher(indexDir.getPath());
                                indexSearcherListBuy.put(indexDir.getPath(), indexSearcher);
                            }
                            break;
                        case PROVISION:
                            if (!Optional.ofNullable(indexSearcherListSell.get(indexDir.getPath())).isPresent()) {
                                IndexSearcher indexSearcher = createIndexSearcher(indexDir.getPath());
                                indexSearcherListSell.put(indexDir.getPath(), indexSearcher);
                            }
                            break;
                    }
                }
            }
        }
    }

    public static IndexSearcher getIndexSearcherByIndexDirName(Project.ProjectType projectType, String indexDirName) {
        IndexSearcher indexSearcher = null;
        String indexpath = getMapKeyByPath(indexDirName);
        switch (projectType) {
            case CONSUMPTION:
                indexSearcher = SearchHelper.indexSearcherListBuy.get(indexpath);
                break;
            case PROVISION:
                indexSearcher = SearchHelper.indexSearcherListSell.get(indexpath);
                break;
        }
        // if existed, get from indexReader
        indexSearcher = refreshIndexSearcherIfChanged(indexSearcher);
        // not existed, create a new one
        if (!Optional.ofNullable(indexSearcher).isPresent()) {
            indexSearcher = createIndexSearcher(indexpath);
            switch (projectType) {
                case PROVISION:
                    indexSearcherListSell.put(indexpath, indexSearcher);
                    break;
                case CONSUMPTION:
                    indexSearcherListBuy.put(indexpath, indexSearcher);
                    break;
            }
        }
        return indexSearcher;
    }

    public static Map<String, IndexSearcher> getAllIndexSearchers(Project.ProjectType projectType) {
//        refreshAllIndexSearcher();
        Map<String, IndexSearcher> indexSearchers = new HashMap<>();
        switch (projectType) {
            case CONSUMPTION:
                indexSearchers = indexSearcherListBuy;
                break;
            case PROVISION:
                indexSearchers = indexSearcherListSell;
                break;
        }
        // check if the index change
        indexSearchers.forEach(
                (dir, indexSearcher) ->
                        getIndexSearcherByIndexDirName(projectType, dir)
        );
        return indexSearchers;
    }

    public static List<IndexSearcher> getAllFileIndexSearcher(Project.ProjectType projectType, BipsProperties properties) {
//        refreshAllIndexSearcher();
        List<IndexSearcher> fileIndexSearcherList = new ArrayList<>();
        Map<String, IndexSearcher> indexSearcherListExisted = new HashMap<>();
        switch (projectType) {
            case CONSUMPTION:
                indexSearcherListExisted = indexSearcherListBuy;
                break;
            case PROVISION:
                indexSearcherListExisted = indexSearcherListSell;
                break;
        }
        indexSearcherListExisted.forEach(
                (dir, indexSearcher) -> {
                    if (isSonDirectory(dir, properties.getProjectBuyLocation())) {
                        fileIndexSearcherList.add(indexSearcher);
                    }
                }
        );
        return fileIndexSearcherList;
    }

    public static List<IndexSearcher> getAllProjectIndexSearcher(Project.ProjectType projectType, BipsProperties properties) {
//        refreshAllIndexSearcher();
        List<IndexSearcher> projectIndexSearcherList = new ArrayList<>();
        switch (projectType) {
            case CONSUMPTION:
                indexSearcherListBuy.forEach(
                        (dir, indexSearcher) -> {
                            if (isSonDirectory(dir, properties.getProjectBuyLocation())) {
                                projectIndexSearcherList.add(refreshIndexSearcherIfChanged(indexSearcher));
                            }
                        }
                );
                break;
            case PROVISION:
                indexSearcherListSell.forEach(
                        (dir, indexSearcher) -> {
                            if (isSonDirectory(dir, properties.getProjectSellLocation())) {
                                projectIndexSearcherList.add(refreshIndexSearcherIfChanged(indexSearcher));
                            }
                        }
                );
                break;
        }
        return projectIndexSearcherList;
    }

    public static Map<String, List<IndexSearcher>> getLimitIndexSearcher(Project.ProjectType projectType, BipsProperties properties) {
        Calendar calendar = Calendar.getInstance();
        long size = 0;
        int month = 0;
        List<IndexSearcher> projectIndexSearcherList = new ArrayList<>();
        List<IndexSearcher> fileIndexSearcherList = new ArrayList<>();
        Map<String, List<IndexSearcher>> indexSearcherMap = new HashMap<>();
        String fileIndexPre = "";
        String projectIndexPre = "";
        switch (projectType) {
            case CONSUMPTION:
                fileIndexPre = properties.getFileBuyLocation();
                projectIndexPre = properties.getProjectBuyLocation();
                break;
            case PROVISION:
                fileIndexPre = properties.getFileSellLocation();
                projectIndexPre = properties.getProjectSellLocation();
                break;
        }
        while (month < properties.getIndexMatchReadMaxMonth() && size < properties.getIndexMatchReadMaxSize()) {
            String fileIndexDir = fileIndexPre + LuceneConfig.getIndexDirDateFomat(calendar.getTime());
            String projectIndexDir = projectIndexPre + LuceneConfig.getIndexDirDateFomat(calendar.getTime());
            projectIndexSearcherList.add(getIndexSearcherByIndexDirName(projectType, new File(projectIndexDir).getPath()));
            fileIndexSearcherList.add(getIndexSearcherByIndexDirName(projectType, new File(fileIndexDir).getPath()));
            calendar.add(Calendar.MONTH, -1);
            size += new File(fileIndexDir).length();
            month++;
        }

        indexSearcherMap.put("file", fileIndexSearcherList);
        indexSearcherMap.put("project", projectIndexSearcherList);
        return indexSearcherMap;
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