package com.lmp.integration.sync.monitoring;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshot périodique de l'état de santé de la synchronisation ERP.
 * <p>
 * Stocké à chaque exécution du {@link SyncHealthMonitor} (toutes les 5 min par défaut).
 * Permet l'historisation, l'affichage de tendances, et la détection de dégradation.
 */
@Entity
@Table(name = "sync_health_snapshot")
public class SyncHealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 20)
    private OverallStatus overallStatus = OverallStatus.HEALTHY;

    @Column(name = "queue_depth", nullable = false)
    private int queueDepth = 0;

    @Column(name = "dead_count", nullable = false)
    private int deadCount = 0;

    @Column(name = "failed_stale_count", nullable = false)
    private int failedStaleCount = 0;

    @Column(name = "unverified_count", nullable = false)
    private int unverifiedCount = 0;

    @Column(name = "success_rate_24h", precision = 5, scale = 4)
    private BigDecimal successRate24h;

    @Column(name = "erp_available", nullable = false)
    private boolean erpAvailable = false;

    @Column(name = "erp_latency_ms")
    private Integer erpLatencyMs;

    @Column(name = "reconciliation_gaps", nullable = false)
    private int reconciliationGaps = 0;

    @Column(name = "last_reconciliation_at")
    private Instant lastReconciliationAt;

    @Column(name = "new_patterns_count", nullable = false)
    private int newPatternsCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_metrics", columnDefinition = "jsonb")
    private Map<String, Object> rawMetrics;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum OverallStatus {
        HEALTHY, DEGRADED, CRITICAL
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public OverallStatus getOverallStatus() { return overallStatus; }
    public void setOverallStatus(OverallStatus overallStatus) { this.overallStatus = overallStatus; }

    public int getQueueDepth() { return queueDepth; }
    public void setQueueDepth(int queueDepth) { this.queueDepth = queueDepth; }

    public int getDeadCount() { return deadCount; }
    public void setDeadCount(int deadCount) { this.deadCount = deadCount; }

    public int getFailedStaleCount() { return failedStaleCount; }
    public void setFailedStaleCount(int failedStaleCount) { this.failedStaleCount = failedStaleCount; }

    public int getUnverifiedCount() { return unverifiedCount; }
    public void setUnverifiedCount(int unverifiedCount) { this.unverifiedCount = unverifiedCount; }

    public BigDecimal getSuccessRate24h() { return successRate24h; }
    public void setSuccessRate24h(BigDecimal successRate24h) { this.successRate24h = successRate24h; }

    public boolean isErpAvailable() { return erpAvailable; }
    public void setErpAvailable(boolean erpAvailable) { this.erpAvailable = erpAvailable; }

    public Integer getErpLatencyMs() { return erpLatencyMs; }
    public void setErpLatencyMs(Integer erpLatencyMs) { this.erpLatencyMs = erpLatencyMs; }

    public int getReconciliationGaps() { return reconciliationGaps; }
    public void setReconciliationGaps(int reconciliationGaps) { this.reconciliationGaps = reconciliationGaps; }

    public Instant getLastReconciliationAt() { return lastReconciliationAt; }
    public void setLastReconciliationAt(Instant lastReconciliationAt) { this.lastReconciliationAt = lastReconciliationAt; }

    public int getNewPatternsCount() { return newPatternsCount; }
    public void setNewPatternsCount(int newPatternsCount) { this.newPatternsCount = newPatternsCount; }

    public Map<String, Object> getRawMetrics() { return rawMetrics; }
    public void setRawMetrics(Map<String, Object> rawMetrics) { this.rawMetrics = rawMetrics; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
