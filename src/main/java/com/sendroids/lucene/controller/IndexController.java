package com.sendroids.lucene.controller;

import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.search.Result;
import com.sendroids.lucene.search.SearchForm;
import com.sendroids.lucene.search.service.IndexService;
import com.sendroids.lucene.search.service.SearchService;
import com.sendroids.lucene.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

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
        }

        return searchForm.getResult();
//        return completeSearchForm(searchForm);
    }


//    private Set<Result> completeSearchForm(SearchForm searchForm) {
//
//        Set<Result> resultSet = searchForm.getResult().stream()
//                .map((Result result) -> {
//                    // db access
//                    final Project projectById = projectService.getProjectById(result.getProjectId()).orElseThrow(
//                            () -> new IllegalArgumentException("error")
//                    );
//
//                    if (Optional.ofNullable(projectById).isPresent()) {
//                        // categoryText
//                        result.setCategoryText(projectById.getCategory().getValue().equals(OTHER) ?
//                                projectById.getCategory().getText() :
//                                messageSource.getMessage(
//                                        projectById.getCategory().getMsg(), null,
//                                        LocaleContextHolder.getLocale()));
//
//                        // fieldText
//                        result.setFieldText(projectById.getFields().stream().map(field -> {
//                            if (field.getValue().equals(OTHER)) {
//                                return field.getText();
//                            } else {
//                                return messageSource.getMessage(
//                                        field.getMsg(), null,
//                                        LocaleContextHolder.getLocale());
//                            }
//                        }).toArray(String[]::new));
//
//                        // phaseText
//                        result.setPhaseText(projectById.getPhases().stream().map(phase -> {
//                            if (phase.getValue().equals(OTHER)) {
//                                return phase.getText();
//                            } else {
//                                return messageSource.getMessage(
//                                        phase.getMsg(), null,
//                                        LocaleContextHolder.getLocale());
//                            }
//                        }).toArray(String[]::new));
//
//                        // scope
//                        result.setScopes(projectById.getScopes().stream().map(scope -> {
//                            if (scope.getValue().equals(OTHER)) {
//                                return scope.getText();
//                            } else {
//                                return messageSource.getMessage(
//                                        scope.getMsg(), null,
//                                        LocaleContextHolder.getLocale());
//                            }
//                        }).toArray(String[]::new));
//
//                        // partnership
//                        result.setPartnership(projectById.getPartnerships().stream().map(partnership -> {
//                            if (partnership.getValue().equals(OTHER)) {
//                                return partnership.getText();
//                            } else {
//                                return messageSource.getMessage(
//                                        partnership.getMsg(),
//                                        null,
//                                        LocaleContextHolder.getLocale());
//                            }
//                        }).toArray(String[]::new));
//
//                        // type
//                        result.setType(projectById.getType());
//
//                        // hasContact
//                        for (Contact contact : dbUserContactsAsConsumer) {
//                            if (contact.getProject().getId().equals(projectById.getId())) {
//                                result.setCid(contact.getId().toString());
//                                break;
//                            }
//                        }
//                    } else {
//                        result = null;
//                    }
//                    return result;
//                }).filter(result -> Optional.ofNullable(result).isPresent())
//                .collect(Collectors.toSet());
//        searchForm.getResult().removeIf(result -> {
//            final boolean[] flag = {true};
//            resultSet.forEach(
//                    results -> {
//                        if (results.getProjectId() == result.getProjectId()) {
//                            flag[0] = false;
//                        }
//                    });
//            return flag[0];
//        });
//        return resultSet;
//    }


}
