package com.sendroids.lucene.search;


import com.sendroids.lucene.entity.Project;
import lombok.Data;

@Data
public class Result implements Comparable {
    private Long projectId;
    private String categoryText;
    private String[] scopes;
    private String[] fieldText;
    private String[] phaseText;
    private String keyword;
    private String brief;
    private String[] partnership;

    private String projectName;
    private Project.ProjectType type;

    private Float score;
    private String advice;
    private String cid;

    @Override
    public int compareTo(Object o){
        Result r1 = (Result) o;
        return this.score.compareTo(r1.getScore());
    }
}
