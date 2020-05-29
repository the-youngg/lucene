package com.sendroids.lucene.search.service;


import com.sendroids.lucene.config.BipsProperties;
import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.entity.Project;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
class MyIndex {
    @Autowired
    private MyField myField;
    @Autowired
    private MySearch mySearch;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private BipsProperties properties;

    /**
     * for initialize index (delete all the index first)
     *
     * @param projectType
     */
    public void initIndex(Project.ProjectType projectType) {
        //check the dir
        File projectDir = null;
        File fileDir = null;
        switch (projectType) {
            case CONSUMPTION:
                projectDir = new File(properties.getProjectBuyLocation());
                fileDir = new File(properties.getFileBuyLocation());
                break;
            case PROVISION:
                projectDir = new File(properties.getProjectSellLocation());
                fileDir = new File(properties.getFileSellLocation());
                break;
        }
        initAndDeleteIndex(projectDir);
        initAndDeleteIndex(fileDir);
    }

    private void initAndDeleteIndex(File dir) {
        //delete the index
        if (dir != null && dir.exists() && dir.isDirectory()) {
            for (File indexDir : dir.listFiles()) {
                if (indexDir.exists() && indexDir.isDirectory()) {
                    IndexWriter indexWriter = null;
                    try {
                        indexWriter = getIndexWriter(indexDir.getPath());
                        indexWriter.deleteAll();
                        if (indexWriter.commit() == -1) {
                            log.error("Lucene deleted " + indexWriter.getDirectory() + " failed");
                        } else {
                            log.info("Lucene deleted all the index in " + indexWriter.getDirectory());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (indexWriter != null) {
                                indexWriter.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    /**
     * create,update,delete index
     *
     * @param flag "create" for add index
     *             "update" for update index
     *             "delete" for delete index
     */
    public void updateIndex(String flag, Project project, Date oldUpdateTime) {
        IndexWriter projectIndexWriter = null;
        IndexWriter fileIndexWriter = null;

        IndexWriter oldProjectIndexWriter = null;
        IndexWriter oldFileIndexWriter = null;
        IndexSearcher oldFileIndexSearcher = null;
        try {
            //ready to write index
            projectIndexWriter = getIndexWriter(getProjectIndexDirName(project.getType(), project.getUpdateTime()));
            fileIndexWriter = getIndexWriter(getFileIndexDirName(project.getType(), project.getUpdateTime()));
            //get old index
            if (!LuceneConfig.getIndexDirDateFomat(project.getUpdateTime())
                    .equals(LuceneConfig.getIndexDirDateFomat(oldUpdateTime))) {
                oldProjectIndexWriter = getIndexWriter(getProjectIndexDirName(project.getType(), oldUpdateTime));
                oldFileIndexWriter = getIndexWriter(getProjectIndexDirName(project.getType(), oldUpdateTime));
            }
            if (!flag.equals("create")) {
                oldFileIndexSearcher = SearchHelper.getIndexSearcherByIndexDirName(project.getType(), getFileIndexDirName(project.getType(), oldUpdateTime));
            }

            addDocumentToIndex(projectIndexWriter, oldProjectIndexWriter, fileIndexWriter, oldFileIndexWriter, oldFileIndexSearcher, flag, project);
        } catch (Exception e) {
            try {
                if (projectIndexWriter != null) {
                    projectIndexWriter.rollback();
                }
                if (fileIndexWriter != null) {
                    fileIndexWriter.rollback();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            log.error("Lucene Project #" + project.getId() + " index failed!");
            e.printStackTrace();
        } finally {
            try {
                if (projectIndexWriter != null) {
                    projectIndexWriter.commit();
                    projectIndexWriter.close();
                }
                if (fileIndexWriter != null) {
                    fileIndexWriter.commit();
                    fileIndexWriter.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private IndexWriter getIndexWriter(String indexDirName) throws IOException {
        return SearchHelper.getIndexWriter(indexDirName);
    }

    private String getProjectIndexDirName(Project.ProjectType projectType, Date date) {
        String indexDirName = "";
        String place = LuceneConfig.getIndexDirDateFomat(date);
        switch (projectType) {
            case CONSUMPTION:
                indexDirName = properties.getProjectBuyLocation() + place;
                break;
            case PROVISION:
                indexDirName = properties.getProjectSellLocation() + place;
                break;
        }
        return indexDirName;
    }

    private String getFileIndexDirName(Project.ProjectType projectType, Date date) {
        String indexDirName = "";
        String place = LuceneConfig.getIndexDirDateFomat(date);
        switch (projectType) {
            case CONSUMPTION:
                indexDirName = properties.getFileBuyLocation() + place;
                break;
            case PROVISION:
                indexDirName = properties.getFileSellLocation() + place;
                break;
        }
        return indexDirName;
    }

//    private Document setFilesToDocument(String docsPath, Document document) throws IOException {
//        File file = new File(docsPath);
//        if (Files.isDirectory(file.toPath()) && file.exists()) {
//            Document tmpDocument = document;
//            Files.walkFileTree(
//                    file.toPath(),
//                    new SimpleFileVisitor<Path>() {
//                        @Override
//                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
//                            setFilesToDocument(path.toAbsolutePath().toString(), tmpDocument);
//                            return FileVisitResult.CONTINUE;
//                        }
//                    });
//
//        } else {
//            document = myField.addFile(document, file);
//        }
//        return document;
//    }

    private void addDocumentToIndex(
            IndexWriter projectIndexWriter,
            IndexWriter oldProjectIndexWriter, //to delete index before
            IndexWriter fileIndexWriter,
            IndexWriter oldFileIndexWriter,//to delete index before
            IndexSearcher oldFileIndexSearcher,//to find the old file, not need to reread
            String flag,
            Project project)
            throws IOException {
        Document projectDocument = new Document();
        Document fileDocument = new Document();
        Optional<Document> documentExisted = Optional.empty();
        switch (flag) {
            case "create":
                projectDocument = myField.addProject(projectDocument, project);
                projectIndexWriter.addDocument(projectDocument);
                fileIndexWriter.addDocument(fileDocument);
                break;
            case "update":
                projectDocument = myField.addProject(projectDocument, project);
                projectIndexWriter.updateDocument(new Term(myField.projectId, project.getId().toString()), projectDocument);
                if (Optional.ofNullable(oldFileIndexSearcher).isPresent()) {
                    List<Document> documents = mySearch.findIndexDocument(oldFileIndexSearcher, myField.projectId, String.valueOf(project.getId()), 1);
                    documentExisted = Optional.ofNullable(documents.isEmpty() ? null : documents.get(0));
                }
                fileIndexWriter.updateDocument(new Term(myField.projectId, project.getId().toString()), fileDocument);
                break;
            case "delete":
                projectIndexWriter.deleteDocuments(new Term(myField.getProjectId(), project.getId().toString()));
                break;
            default:
                break;
        }
        if (Optional.ofNullable(oldProjectIndexWriter).isPresent()) {
            oldProjectIndexWriter.deleteDocuments(new Term(myField.getProjectId(), project.getId().toString()));
            if (oldProjectIndexWriter.commit() == -1) {
                log.error("Lucene deleting old index Project #" + project.getId() + " index failed!");
            } else {
                log.info("Lucene deleting old index Project #" + project.getId() + " index success.");
            }
        }
        if (Optional.ofNullable(oldFileIndexWriter).isPresent()) {
            oldFileIndexWriter.deleteDocuments(new Term(myField.getProjectId(), project.getId().toString()));
            if (oldFileIndexWriter.commit() == -1) {
                log.error("Lucene deleting old file index Project #" + project.getId() + " index failed, might not existed!");
            } else {
                log.info("Lucene deleting old file index Project #" + project.getId() + " index success.");
            }
        }
        if (projectIndexWriter.commit() == -1) {
            log.error("Lucene index Project #" + project.getId() + " failed!");
        }
        if (fileIndexWriter.commit() == -1) {
            log.error("Lucene index file #" + project.getId() + " failed!");
        }
    }

}
