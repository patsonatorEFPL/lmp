package com.lmp.project.repository;

import com.lmp.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByExternalProjectId(String externalProjectId);

    List<Project> findByCustomerId(UUID customerId);

    List<Project> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
