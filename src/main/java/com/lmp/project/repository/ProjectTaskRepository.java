package com.lmp.project.repository;

import com.lmp.project.domain.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {

    Optional<ProjectTask> findByExternalTaskId(String externalTaskId);

    List<ProjectTask> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    @Query("SELECT t FROM ProjectTask t JOIN FETCH t.project WHERE t.id = :id")
    Optional<ProjectTask> findByIdWithProject(@Param("id") UUID id);
}
