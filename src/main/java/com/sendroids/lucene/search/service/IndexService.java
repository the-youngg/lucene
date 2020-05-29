package com.sendroids.lucene.search.service;


import com.sendroids.lucene.entity.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class IndexService {
    private final MyIndex myIndex;

    @Autowired
    public IndexService(MyIndex myIndex) {
        this.myIndex = myIndex;
    }

    public void createIndex(Project project) {
        Date start = new Date();
        if (Optional.ofNullable(project).isPresent() && project.getId() != null) {
            myIndex.updateIndex("create", project, project.getUpdateTime());
            log.info("Lucene index create #" + project.getId() + " cost " + (new Date().getTime() - start.getTime()) + "ms");
        } else {
            log.error("Lucene create index NULL project failed, " + project);
        }
    }

    /**
     *
     * @param project the project updated
     * @param oldUpdateTime to get the old index location (use updateTime for now)
     */
    public void updateIndex(Project project, Date oldUpdateTime) {
        Date start = new Date();
        if (Optional.ofNullable(project).isPresent() && project.getId() != null) {
            myIndex.updateIndex("update", project, oldUpdateTime);
            log.info("Lucene index update #" + project.getId() + " cost " + (new Date().getTime() - start.getTime()) + "ms");
        } else {
            log.error("Lucene update index NULL project failed, " + project);
        }
    }

    public void deleteIndex(Project project) {
        Date start = new Date();
        if (Optional.ofNullable(project).isPresent() && project.getId() != null) {
            myIndex.updateIndex("delete", project, project.getUpdateTime());
            log.info("Lucene index delete #" + project.getId() + " cost " + (new Date().getTime() - start.getTime()) + "ms");
        } else {
            log.error("Lucene delete index NULL project failed, " + project);
        }
    }

    public void initIndex(Project.ProjectType projectType) {
        myIndex.initIndex(projectType);
    }

//    public void addDictionary(Project.ProjectType projectType, File dictionary) {
//        myIndex.addDictionary(projectType, dictionary);
//    }
}
