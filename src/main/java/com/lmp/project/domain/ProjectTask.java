package com.lmp.project.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tâche d'un projet — synchronisée avec le système externe.
 */
@Entity
@Table(name = "project_tasks")
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String status = "Open";

    @Column(length = 20)
    private String priority = "Medium";

    private Integer progress = 0;

    @Column(name = "expected_start_date")
    private LocalDate expectedStartDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "external_task_id", length = 140)
    private String externalTaskId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ProjectTask() {}

    public ProjectTask(String title, Project project) {
        this.title = title;
        this.project = project;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters / Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public LocalDate getExpectedStartDate() { return expectedStartDate; }
    public void setExpectedStartDate(LocalDate expectedStartDate) { this.expectedStartDate = expectedStartDate; }

    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getExternalTaskId() { return externalTaskId; }
    public void setExternalTaskId(String externalTaskId) { this.externalTaskId = externalTaskId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
