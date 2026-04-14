package com.lmp.shared.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Enregistre passivement les métriques de santé des APIs externes.
 *
 * <p>Double écriture :
 * <ul>
 *   <li>En mémoire (fenêtre glissante 30 min) — pour le dashboard temps réel</li>
 *   <li>En base (fenêtre 24h) — pour la génération de rapports quotidiens</li>
 * </ul>
 *
 * <p>L'écriture en base est asynchrone : les records sont collectés dans une queue
 * et flushés toutes les 60 secondes pour minimiser la pression sur la DB.
 *
 * <p>Thread-safe : utilise {@link ConcurrentHashMap} + synchronisation sur les listes internes.
 */
@Component
public class ApiHealthRecorder {

    private static final Logger logger = LoggerFactory.getLogger(ApiHealthRecorder.class);

    /** Fenêtre de rétention mémoire (30 min). */
    private static final long WINDOW_MS = 30 * 60 * 1000L;

    /** Seuils de statut. */
    private static final double SUCCESS_RATE_UP = 0.90;
    private static final double SUCCESS_RATE_DEGRADED = 0.50;
    private static final long LATENCY_DEGRADED_MS = 3000;
    private static final long STALE_THRESHOLD_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, List<CallRecord>> records = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ApiHealthRecord> pendingDbWrites = new ConcurrentLinkedQueue<>();

    private final ApiHealthRecordRepository recordRepository;

    public ApiHealthRecorder(ApiHealthRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * Enregistre un appel vers une API externe.
     *
     * @param apiName   identifiant unique de l'API (ex: "ip-api.com")
     * @param latencyMs durée de l'appel en millisecondes
     * @param success   {@code true} si l'appel a réussi
     * @param error     message d'erreur (null si succès)
     */
    public void record(String apiName, long latencyMs, boolean success, String error) {
        // 1. Écriture mémoire (temps réel)
        records.computeIfAbsent(apiName, k -> new ArrayList<>());
        List<CallRecord> list = records.get(apiName);
        synchronized (list) {
            list.add(new CallRecord(Instant.now(), latencyMs, success, error));
            Instant cutoff = Instant.now().minusMillis(WINDOW_MS);
            list.removeIf(r -> r.timestamp().isBefore(cutoff));
        }

        // 2. Queue pour écriture DB asynchrone (null-safe pour les tests unitaires)
        if (recordRepository != null) {
            pendingDbWrites.add(new ApiHealthRecord(apiName, (int) latencyMs, success, error));
        }
    }

    /**
     * Flush les records en attente vers la base toutes les 60 secondes.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void flushToDatabase() {
        if (recordRepository == null) return;
        List<ApiHealthRecord> batch = new ArrayList<>();
        ApiHealthRecord record;
        while ((record = pendingDbWrites.poll()) != null) {
            batch.add(record);
        }
        if (!batch.isEmpty()) {
            try {
                recordRepository.saveAll(batch);
                logger.debug("[API-HEALTH] Flushed {} records to database", batch.size());
            } catch (Exception e) {
                logger.warn("[API-HEALTH] Failed to flush records to DB: {}", e.getMessage());
                // Re-queue failed records
                pendingDbWrites.addAll(batch);
            }
        }
    }

    /**
     * Retourne un snapshot des métriques pour toutes les APIs enregistrées.
     */
    public Map<String, ApiHealthEntry> getSnapshot() {
        Map<String, ApiHealthEntry> snapshot = new LinkedHashMap<>();
        Instant now = Instant.now();
        Instant cutoff = now.minusMillis(WINDOW_MS);

        for (var entry : records.entrySet()) {
            String apiName = entry.getKey();
            List<CallRecord> list = entry.getValue();

            long totalCalls;
            long successCount;
            long totalLatency;
            Instant lastCallAt = null;
            String lastError = null;

            synchronized (list) {
                list.removeIf(r -> r.timestamp().isBefore(cutoff));

                totalCalls = list.size();
                successCount = list.stream().filter(CallRecord::success).count();
                totalLatency = list.stream().mapToLong(CallRecord::latencyMs).sum();

                for (int i = list.size() - 1; i >= 0; i--) {
                    CallRecord r = list.get(i);
                    if (lastCallAt == null) {
                        lastCallAt = r.timestamp();
                    }
                    if (lastError == null && r.error() != null) {
                        lastError = r.error();
                    }
                    if (lastCallAt != null && lastError != null) break;
                }
            }

            double avgLatency = totalCalls > 0 ? (double) totalLatency / totalCalls : 0;
            double successRate = totalCalls > 0 ? (double) successCount / totalCalls : 0;

            String status;
            if (totalCalls == 0) {
                status = "UNKNOWN";
            } else {
                boolean stale = lastCallAt != null
                        && lastCallAt.plusMillis(STALE_THRESHOLD_MS).isBefore(now);
                if (stale || successRate < SUCCESS_RATE_DEGRADED) {
                    status = "DOWN";
                } else if (successRate < SUCCESS_RATE_UP || avgLatency > LATENCY_DEGRADED_MS) {
                    status = "DEGRADED";
                } else {
                    status = "UP";
                }
            }

            snapshot.put(apiName, new ApiHealthEntry(
                    apiName,
                    status,
                    Math.round(avgLatency),
                    Math.round(successRate * 1000.0) / 10.0,
                    totalCalls,
                    lastCallAt != null ? lastCallAt.toString() : null,
                    lastError
            ));
        }

        return snapshot;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private record CallRecord(Instant timestamp, long latencyMs, boolean success, String error) {}
}
