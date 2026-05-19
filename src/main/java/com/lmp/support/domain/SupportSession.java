package com.lmp.support.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remote HelpDesk session linking a {@link Ticket} customer to a tech.
 * Transitions DRAFT → INVITED → CONSENT_WAIT → ACTIVE → ENDING → MUXING → ARCHIVED,
 * with ABORTED as a side-exit reachable from any non-terminal state.
 */
@Entity
@Table(name = "support_session")
public class SupportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tech_user_id", nullable = false)
    private UUID techUserId;

    @Column(name = "client_user_id", nullable = false)
    private UUID clientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.DRAFT;

    @Column(name = "mesh_central_group_id", length = 128)
    private String meshCentralGroupId;

    @Column(name = "mesh_central_node_id", length = 128)
    private String meshCentralNodeId;

    @Column(name = "runner_token_jti", nullable = false, unique = true)
    private UUID runnerTokenJti;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "consent_at")
    private LocalDateTime consentAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "end_reason", length = 64)
    private String endReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Version
    private long version;

    public SupportSession() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }

    public UUID getTechUserId() { return techUserId; }
    public void setTechUserId(UUID techUserId) { this.techUserId = techUserId; }

    public UUID getClientUserId() { return clientUserId; }
    public void setClientUserId(UUID clientUserId) { this.clientUserId = clientUserId; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public String getMeshCentralGroupId() { return meshCentralGroupId; }
    public void setMeshCentralGroupId(String meshCentralGroupId) { this.meshCentralGroupId = meshCentralGroupId; }

    public String getMeshCentralNodeId() { return meshCentralNodeId; }
    public void setMeshCentralNodeId(String meshCentralNodeId) { this.meshCentralNodeId = meshCentralNodeId; }

    public UUID getRunnerTokenJti() { return runnerTokenJti; }
    public void setRunnerTokenJti(UUID runnerTokenJti) { this.runnerTokenJti = runnerTokenJti; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }

    public LocalDateTime getConsentAt() { return consentAt; }
    public void setConsentAt(LocalDateTime consentAt) { this.consentAt = consentAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
