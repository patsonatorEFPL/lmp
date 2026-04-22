package com.lmp.project.service;

import com.lmp.project.domain.Project;
import com.lmp.project.domain.ProjectTask;
import com.lmp.project.repository.ProjectRepository;
import com.lmp.project.repository.ProjectTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service métier pour la gestion des projets et tâches.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectTaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public List<Project> getProjectsByCustomer(UUID customerId) {
        return projectRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Optional<Project> getProject(UUID id) {
        return projectRepository.findById(id);
    }

    public Optional<Project> getByExternalId(String externalProjectId) {
        return projectRepository.findByExternalProjectId(externalProjectId);
    }

    @Transactional
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    public List<ProjectTask> getTasksByProject(UUID projectId) {
        return taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    public Optional<ProjectTask> getTaskByExternalId(String externalTaskId) {
        return taskRepository.findByExternalTaskId(externalTaskId);
    }

    @Transactional
    public ProjectTask saveTask(ProjectTask task) {
        return taskRepository.save(task);
    }
}
