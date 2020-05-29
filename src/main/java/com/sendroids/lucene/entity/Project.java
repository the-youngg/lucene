package com.sendroids.lucene.entity;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Setter
@EqualsAndHashCode
public class Project implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column
    protected String name;

    @Column(length = 1024)
    private String brief;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    @Column(name = "register_time")
    private Date registerTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    @Column(name = "update_time")
    private Date updateTime = registerTime;

    @Column(length = 1024)
    private String content;


    @Override
    public String toString() {
        return "Project@" + hashCode() +
                "[id=" + id +
                ", name=" + name +
                ", brief=" + brief +
                ", updateTime=" + updateTime +
                ", registerTime=" + registerTime +
                ", content=" + content +
                "]";
    }

}
