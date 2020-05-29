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

    private boolean publish = true;

    @Column(length = 1024)
    private String brief;

    private ProjectType type = ProjectType.PROVISION;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    @Column(name = "register_time")
    private Date registerTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    @Column(name = "update_time")
    private Date updateTime = registerTime;

    @Column(name = "dev_code")
    private String devCode = "";

    @Column(length = 1024)
    private String symptom = "";

    @Column(length = 1024)
    private String dosage = "";

    @Column(length = 1024)
    private String effect = "";

    private Long hits = 0L;


    @Override
    public String toString() {
        return "Project@" + hashCode() +
                "[id=" + id +
                ", name=" + name +
                ", publish=" + publish +
                ", brief=" + brief +
                ", type=" + type +
                ", updateTime=" + updateTime +
                ", registerTime=" + registerTime +
                ", devCode=" + devCode +
                ", symptom=" + symptom +
                ", dosage=" + dosage +
                ", effect=" + effect +
                "]";
    }

    public enum ProjectType {
        NEITHER(0 , "Asset none"),
        PROVISION(1, "Provider projects"),
        CONSUMPTION(2, "Seeker projects"),
        BOTH(3, "Provider projects & Seeker projects");

        private int key;
        private String txt;

        ProjectType(int key, String txt) {
            this.key = key;
            this.txt = txt;
        }

        public int getKey() {
            return this.key;
        }
        public String getText() {
            return this.txt;
        }

    }

}
