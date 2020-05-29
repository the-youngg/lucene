package com.sendroids.lucene.search.service;


import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.search.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class SearchService {
    private final MySearch mySearch;

    @Autowired
    public SearchService(MySearch mySearch) {
        this.mySearch = mySearch;
    }

    public Collection<Result> numSearch(
            final String keyword,
            final int max
    ) {
        return mySearch.search(keyword.length() >= 100 ? keyword.substring(0, 100) : keyword,
                max);
    }

}
