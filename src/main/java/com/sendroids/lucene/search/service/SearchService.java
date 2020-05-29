package com.sendroids.lucene.search.service;


import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.search.Result;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class SearchService {
    private final MySearch mySearch;
    private final MyField myField;

    @Autowired
    public SearchService(MySearch mySearch, MyField myField) {
        this.mySearch = mySearch;
        this.myField = myField;
    }

    public Collection<Result> numSearch(
            final String keyword,
            final int max,
            final Project.ProjectType projectType
    ) {
        return mySearch.search(keyword.length() >= 100 ? keyword.substring(0, 100) : keyword,
                max, projectType);
    }


    public Optional<Project> findIndexProjectByProjectId(Long projectId) {
        List<Project> projects = mySearch.findIndexProject(myField.getProjectId(), projectId.toString(), 1);
        return Optional.ofNullable(projects.isEmpty() ? null : projects.get(0));
    }

    public Optional<Document> findIndexDocumentByProjectId(Long projectId) {
        List<Document> documents = mySearch.findIndexDocument(myField.getProjectId(), projectId.toString(), 1);
        return Optional.ofNullable(documents.isEmpty() ? null : documents.get(0));
    }

    public Collection<Result> removeTheSameProject(Collection<Result> list1, Collection<Result> list2) {
        return mySearch.removeTheSameProjectInResult(list1, list2);
    }
}
