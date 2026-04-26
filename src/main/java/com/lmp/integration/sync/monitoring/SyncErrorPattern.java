package com.lmp.integration.sync.monitoring;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pattern d'erreur de synchronisation identifié.
 * <p>
 * Catalogue les erreurs rencontrées pour classifier les nouvelles (UNKNOWN)
 * vs les connues (KNOWN_RECOVERABLE / KNOWN_PERMANENT).
 * Permet la détection de drift côté external ERP.
 */
@Entity
@Table(name = "sync_error_patterns")
public class SyncErrorPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "pattern", nullable = false, length = 500)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category = Category.UNKNOWN;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private LocalDateTime firstSeenAt = LocalDateTime.now();

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    @Column(name = "occurrences", nullable = false)
    private int occurrences = 1;

    @Column(name = "last_sync_event_id")
    private UUID lastSyncEventId;

    @Column(name = "alerted", nullable = false)
    private boolean alerted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Category {
        KNOWN_RECOVERABLE,
        KNOWN_PERMANENT,
        UNKNOWN
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(LocalDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public int getOccurrences() { return occurrences; }
    public void setOccurrences(int occurrences) { this.occurrences = occurrences; }

    public UUID getLastSyncEventId() { return lastSyncEventId; }
    public void setLastSyncEventId(UUID lastSyncEventId) { this.lastSyncEventId = lastSyncEventId; }

    public boolean isAlerted() { return alerted; }
    public void setAlerted(boolean alerted) { this.alerted = alerted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
