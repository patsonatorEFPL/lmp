package com.lmp.support.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "support_consent_record")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private boolean accepted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_consented", nullable = false, columnDefinition = "jsonb")
    private List<String> itemsConsented = new ArrayList<>();

    /**
     * Stored as Postgres {@code inet} but exposed as String for portability with
     * Testcontainers/H2 environments.
     */
    @Column(name = "client_ip", columnDefinition = "inet")
    private String clientIp;

    @Column(name = "client_user_agent", columnDefinition = "TEXT")
    private String clientUserAgent;

    @Column(name = "consent_text_hash", nullable = false, length = 64)
    private String consentTextHash;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    public ConsentRecord() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public List<String> getItemsConsented() { return itemsConsented; }
    public void setItemsConsented(List<String> itemsConsented) { this.itemsConsented = itemsConsented; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getClientUserAgent() { return clientUserAgent; }
    public void setClientUserAgent(String clientUserAgent) { this.clientUserAgent = clientUserAgent; }

    public String getConsentTextHash() { return consentTextHash; }
    public void setConsentTextHash(String consentTextHash) { this.consentTextHash = consentTextHash; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
