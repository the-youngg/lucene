package com.sendroids.lucene.search;


import lombok.Data;

@Data
public class Result implements Comparable {

    private Long projectId;
    private String projectName;
    private String content;
    private String keyword;
    private String brief;
    private Float score;


    @Override
    public int compareTo(Object o) {
        Result r1 = (Result) o;
        return this.score.compareTo(r1.getScore());
    }
}
