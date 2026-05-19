package com.lmp.support.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only audit row. INSERT-only at the DB level via
 * {@code support_audit_log_block_modifications} trigger. SHA-256 hash chain
 * computed application-side links each entry to its predecessor.
 */
@Entity
@Table(name = "support_audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "occurred_at_micros", nullable = false)
    private long occurredAtMicros;

    @Column(name = "prev_hash")
    private byte[] prevHash;

    @Column(name = "entry_hash", nullable = false)
    private byte[] entryHash;

    public AuditLogEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public long getOccurredAtMicros() { return occurredAtMicros; }
    public void setOccurredAtMicros(long occurredAtMicros) { this.occurredAtMicros = occurredAtMicros; }

    public byte[] getPrevHash() { return prevHash; }
    public void setPrevHash(byte[] prevHash) { this.prevHash = prevHash; }

    public byte[] getEntryHash() { return entryHash; }
    public void setEntryHash(byte[] entryHash) { this.entryHash = entryHash; }
}
