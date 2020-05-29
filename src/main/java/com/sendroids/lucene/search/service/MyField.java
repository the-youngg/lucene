package com.sendroids.lucene.search.service;


import com.sendroids.lucene.entity.Project;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.springframework.stereotype.Component;

/**
 * use when creating index
 * for adding doc's field to the index
 * field  e.g. title, content, name
 */
@Component
@Slf4j
@Data
public class MyField {
    public final String projectId = "projectId";
    public final String projectName = "projectName";
    public final String keyword = "keyword";
    public final String brief = "brief";


    public Document addProject(Document document, Project project) {
        StringField pid = new StringField(projectId, project.getId().toString(), Field.Store.YES);
        StringField pName = new StringField(projectName, project.getName(), Field.Store.YES);

        Field pBrief = new Field(brief, project.getBrief(), TextField.TYPE_STORED);
        Field name = new Field("name", project.getName(), TextField.TYPE_STORED);

        document.add(pid);
        document.add(pName);
        document.add(pBrief);
        document.add(name);

        System.out.println(document);
        log.info("Lucene Index Project#" + project.getId() + ": " + document);
        return document;
    }


}
