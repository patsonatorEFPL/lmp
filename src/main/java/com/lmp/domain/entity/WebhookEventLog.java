package com.lmp.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entité pour l'idempotence des webhooks.
 * Stocke les IDs d'événements déjà traités pour éviter les doublons.
 */
@Entity
@Table(name = "webhook_event_logs")
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    // Constructors
    public WebhookEventLog() {}

    public WebhookEventLog(String eventId, String eventType, String provider) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.provider = provider;
        this.processedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProcessingError() { return processingError; }
    public void setProcessingError(String processingError) { this.processingError = processingError; }
}
