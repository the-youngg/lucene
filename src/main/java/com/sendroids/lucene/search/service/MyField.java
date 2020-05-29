package com.sendroids.lucene.search.service;


import com.sendroids.lucene.config.LuceneConfig;
import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.service.ProjectService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

/**
 * use when creating index
 * for adding doc's field to the index
 * field  e.g. title, content, name
 */
@Component
@Slf4j
@Data
public class MyField {
    public final String userId = "userId";
    public final String projectId = "projectId";
    public final String projectName = "projectName";
    public final String keyword = "keyword";
    public final String brief = "brief";
    public final String projectField = "projectField";
    public final String projectScope = "scope";
    public final String projectCategory = "category";
    public final String projectPartner = "partner";
    public final String projectPhase = "phase";
    public final String projectIP = "IP";
    public final String type = "type";
    public final String fileContent = "fileContent";
    public final String filename = "filename";
    public final String filepath = "filepath";
    public final String dictionary = "dictionary";

    @Autowired
    public final LuceneConfig luceneConfig;
    @Autowired
    public final Analyzer myAnalyzer;
    @Autowired
    private MessageSource messageSource;
    //    private final String score = "score";
//    private String id = "id";
    public final ProjectService projectService;

    /**
     * StringField 需要全匹配，适合路径、文件名
     * Field 分词匹配
     */
    public Document addFile(Document document, File file, Project project) {
        Date date = new Date();
        document.add(new StringField(projectId, project.getId().toString(), Field.Store.YES));
        if (file.exists()) {
            document.add(new StringField(filename, file.getName(), Field.Store.YES));
            document.add(new StringField(filepath, file.getPath(), Field.Store.YES));
//            MyQueryUtil.showToken(MyFileReaderUtil.readFile(file), fileContent, myAnalyzer);
            log.info("Read file [" + file.getName() + "] cost " + (new Date().getTime() - date.getTime()) + "ms");
        } else {
            document.add(new StringField(filename, "", Field.Store.YES));
            document.add(new StringField(filepath, "", Field.Store.YES));
            document.add(new TextField(fileContent, "", Field.Store.YES));
        }
//        System.out.println(document);
        return document;
    }

    public Document addStringField(Document document, String field, String value, Field.Store store) {
        document.add(new StringField(field, value, store));
        return document;
    }

    public Document addField(Document document, String field, String value, FieldType store) {
        document.add(new Field(field, value, store));
        return document;
    }

    public Document addProject(Document document, Project project) {
        StringField pid = new StringField(projectId, project.getId().toString(), Field.Store.YES);
        StringField pName = new StringField(projectName, project.getName(), Field.Store.YES);
        StringField pType = new StringField(type, project.getType().name(), Field.Store.YES);

        Field pBrief = new Field(brief, project.getBrief(), TextField.TYPE_STORED);
        Field name = new Field("name", project.getName(), TextField.TYPE_STORED);


//        TextField pName = new TextField(projectName, project.getName(), Field.Store.YES);
//        StringField pType = new StringField(type, project.getType().name(), Field.Store.YES);
//
//        TextField pKeyword = new TextField(keyword, collectTags(project.getTags()), Field.Store.YES);
//        TextField pBrief = new TextField(brief, project.getBrief(), Field.Store.YES);
//        TextField pScope = new TextField(projectScope, collectScopes(project.getScopes()), Field.Store.YES);
//        TextField pCategory = new TextField(projectCategory, project.getCategory().getText(), Field.Store.YES);
//        TextField pField = new TextField(projectField, collectFields(project.getFields()), Field.Store.YES);

//        MyQueryUtil.showToken(collectTags(), keyword);
//        MyQueryUtil.showToken(project.getBrief(), brief);


        document.add(pid);
        document.add(pName);
        document.add(pBrief);
        document.add(pType);
        document.add(name);


        System.out.println(document);
        log.info("Lucene Index Project#" + project.getId() + ": " + document);
        return document;
    }




}
