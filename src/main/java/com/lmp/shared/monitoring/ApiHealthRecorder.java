package com.lmp.shared.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Enregistre passivement les métriques de santé des APIs externes.
 *
 * <p>Chaque appel HTTP externe est signalé via {@link #record(String, long, boolean, String)}.
 * Les métriques sont conservées en mémoire dans une fenêtre glissante de 30 minutes.
 *
 * <p>Thread-safe : utilise {@link ConcurrentHashMap} + synchronisation sur les listes internes.
 */
@Component
public class ApiHealthRecorder {

    /** Fenêtre de rétention des métriques (30 min). */
    private static final long WINDOW_MS = 30 * 60 * 1000L;

    /** Seuils de statut. */
    private static final double SUCCESS_RATE_UP = 0.90;
    private static final double SUCCESS_RATE_DEGRADED = 0.50;
    private static final long LATENCY_DEGRADED_MS = 3000;
    private static final long STALE_THRESHOLD_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, List<CallRecord>> records = new ConcurrentHashMap<>();

    /**
     * Enregistre un appel vers une API externe.
     *
     * @param apiName   identifiant unique de l'API (ex: "ip-api.com")
     * @param latencyMs durée de l'appel en millisecondes
     * @param success   {@code true} si l'appel a réussi
     * @param error     message d'erreur (null si succès)
     */
    public void record(String apiName, long latencyMs, boolean success, String error) {
        records.computeIfAbsent(apiName, k -> new ArrayList<>());
        List<CallRecord> list = records.get(apiName);
        synchronized (list) {
            list.add(new CallRecord(Instant.now(), latencyMs, success, error));
            // Purge les entrées hors fenêtre
            Instant cutoff = Instant.now().minusMillis(WINDOW_MS);
            list.removeIf(r -> r.timestamp().isBefore(cutoff));
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
                // Purge avant lecture
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
                    Math.round(successRate * 1000.0) / 10.0, // 1 decimal %
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
