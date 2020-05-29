package com.sendroids.lucene.repository;

import com.sendroids.lucene.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
