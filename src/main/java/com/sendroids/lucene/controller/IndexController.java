package com.sendroids.lucene.controller;

import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.search.Result;
import com.sendroids.lucene.search.SearchForm;
import com.sendroids.lucene.search.service.IndexService;
import com.sendroids.lucene.search.service.SearchService;
import com.sendroids.lucene.service.ProjectService;
import lombok.val;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class IndexController {

    private static final String OTHER = "990";
    private static final String DIGITAL_NUMBER = "^[0-9]*$";

    private final IndexService indexService;
    private final SearchService searchService;
    private final ProjectService projectService;

    public IndexController(
            IndexService indexService,
            SearchService searchService,
            ProjectService projectService) {
        this.indexService = indexService;
        this.searchService = searchService;
        this.projectService = projectService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello world!";
    }


    @PostMapping("/create")
    public void create(
            final @RequestBody Project project
    ) {

        projectService.update(project);
        indexService.createIndex(project);
    }

    @PutMapping("/update")
    public void update(
            final @RequestBody Project project
    ) {
        projectService.update(project);
        indexService.updateIndex(project, new Date());
    }


    @PostMapping("/delete/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        val dbProject = projectService.getProjectById(id).orElseThrow(
                () -> new IllegalArgumentException(id + "is not exist")
        );
//        projectService.deleteProject(dbProject);
        indexService.deleteIndex(dbProject);
    }

    @GetMapping("/search")
    public Collection<Result> search(
            final @RequestBody SearchForm searchForm
    ) {
        final String kw = searchForm.getKeyword();

        if (!kw.isEmpty()) {
            final long start = System.currentTimeMillis();

            // search and set results in searchForm
            searchForm.setResult(searchService.numSearch(
                    kw, // search keyword
                    1000 // max result count
            ));
            final long costTime = System.currentTimeMillis() - start;

            searchForm.setCostTime(costTime);
            searchForm.setKeyword(kw);
        }


        return completeSearchForm(searchForm);
    }

    private Set<Result> completeSearchForm(
            final SearchForm searchForm
    ) {
        return searchForm.getResult()
                .stream()
                .map(result -> {
                    final val dbProject = projectService.getProjectById(result.getProjectId()).orElseGet(Project::new);
                    result.setProjectName(dbProject.getName());
                    result.setContent(dbProject.getContent());
                    result.setKeyword(searchForm.getKeyword());
                    return result;
                })
                .collect(Collectors.toSet());
    }


}
