package com.lmp.support.domain;

import com.lmp.auth.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ticket de support — synchronisé avec le système externe (Issue).
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(name = "ticket_type", length = 50)
    private String ticketType;

    @Column(name = "resolution_details", columnDefinition = "TEXT")
    private String resolutionDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "external_issue_id", length = 140)
    private String externalIssueId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Ticket() {}

    public Ticket(String subject, String description, User customer) {
        this.subject = subject;
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

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }

    public String getResolutionDetails() { return resolutionDetails; }
    public void setResolutionDetails(String resolutionDetails) { this.resolutionDetails = resolutionDetails; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public String getExternalIssueId() { return externalIssueId; }
    public void setExternalIssueId(String externalIssueId) { this.externalIssueId = externalIssueId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
