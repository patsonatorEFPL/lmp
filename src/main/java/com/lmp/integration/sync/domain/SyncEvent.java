package com.lmp.integration.sync.domain;

import com.lmp.integration.sync.SyncDirection;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Journal de synchronisation — chaque opération (entrante ou sortante) est tracée.
 * Permet l'audit, le debugging, le retry automatique et la réconciliation.
 */
@Entity
@Table(name = "sync_event_log")
public class SyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SyncDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private SyncEntityType entityType;

    @Column(name = "local_entity_id")
    private UUID localEntityId;

    @Column(name = "external_entity_id", length = 140)
    private String externalEntityId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status = SyncStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 5;

    @Column(name = "operation", length = 20)
    private String operation = "CREATE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** Horodatage de la vérification post-sync (GET côté système externe). Null = pas encore vérifié. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** Message d'erreur de vérification (docstatus incorrect, montant divergent, etc.). */
    @Column(name = "verification_error", columnDefinition = "TEXT")
    private String verificationError;

    public SyncEvent() {}

    // --- Getters / Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SyncDirection getDirection() { return direction; }
    public void setDirection(SyncDirection direction) { this.direction = direction; }

    public SyncEntityType getEntityType() { return entityType; }
    public void setEntityType(SyncEntityType entityType) { this.entityType = entityType; }

    public UUID getLocalEntityId() { return localEntityId; }
    public void setLocalEntityId(UUID localEntityId) { this.localEntityId = localEntityId; }

    public String getExternalEntityId() { return externalEntityId; }
    public void setExternalEntityId(String externalEntityId) { this.externalEntityId = externalEntityId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerificationError() { return verificationError; }
    public void setVerificationError(String verificationError) { this.verificationError = verificationError; }
}
