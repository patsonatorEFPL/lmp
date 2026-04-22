package com.lmp.project.domain;

import com.lmp.auth.domain.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Projet client — synchronisé avec le système externe.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.OPEN;

    @Column(name = "percent_complete")
    private Integer percentComplete = 0;

    @Column(name = "expected_start_date")
    private LocalDate expectedStartDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "external_project_id", length = 140)
    private String externalProjectId;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTask> tasks = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Project() {}

    public Project(String title, String description, User customer) {
        this.title = title;
        this.description = description;
        this.customer = customer;
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

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Integer getPercentComplete() { return percentComplete; }
    public void setPercentComplete(Integer percentComplete) { this.percentComplete = percentComplete; }

    public LocalDate getExpectedStartDate() { return expectedStartDate; }
    public void setExpectedStartDate(LocalDate expectedStartDate) { this.expectedStartDate = expectedStartDate; }

    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }

    public LocalDate getActualStartDate() { return actualStartDate; }
    public void setActualStartDate(LocalDate actualStartDate) { this.actualStartDate = actualStartDate; }

    public LocalDate getActualEndDate() { return actualEndDate; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public String getExternalProjectId() { return externalProjectId; }
    public void setExternalProjectId(String externalProjectId) { this.externalProjectId = externalProjectId; }

    public List<ProjectTask> getTasks() { return tasks; }
    public void setTasks(List<ProjectTask> tasks) { this.tasks = tasks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
