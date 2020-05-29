package com.sendroids.lucene.search;


import com.sendroids.lucene.entity.Project;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class SearchForm {

    @NonNull
    private String keyword = "";

    private Long costTime;

    private Collection<Result> result = new ArrayList<>();

    private Collection<Project> project;
}
