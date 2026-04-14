package com.lmp.shared.monitoring;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Enregistrement persistant d'un appel API externe.
 * Conservé 24h en base pour la génération de rapports quotidiens.
 */
@Entity
@Table(name = "api_health_records")
public class ApiHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ApiHealthRecord() {}

    public ApiHealthRecord(String apiName, int latencyMs, boolean success, String error) {
        this.apiName = apiName;
        this.latencyMs = latencyMs;
        this.success = success;
        this.error = error;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getApiName() { return apiName; }
    public int getLatencyMs() { return latencyMs; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public Instant getRecordedAt() { return recordedAt; }
}
