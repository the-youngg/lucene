package com.sendroids.lucene.service;


import com.sendroids.lucene.entity.Project;
import com.sendroids.lucene.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public void deleteProject(
            final Project project
    ){
        projectRepository.delete(project);
    }

    public void save(Project project) {
        projectRepository.save(project);
    }

    public Collection<Project> getAllProjects() {
        return projectRepository.findAll();
    }


    public Project update(Project project) {
        project.setUpdateTime(new Date());
        return projectRepository.save(project);
    }

}
