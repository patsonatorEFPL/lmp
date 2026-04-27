package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.shared.monitoring.ApiHealthRecorder;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Endpoint API pour monitorer la connexion Sentry.
 * <p>
 * Exposé sur {@code /api/sentry/health} — accessible sans authentification
 * pour les health checks externes (Dokploy, UptimeRobot, etc.).
 * <p>
 * Le heartbeat est envoyé automatiquement par {@link SyncHealthMonitor}
 * à chaque snapshot (toutes les 5 min).
 */
@RestController
@RequestMapping("/api/sentry")
public class SentryHealthController {

    private static final Logger log = LoggerFactory.getLogger(SentryHealthController.class);
    private static final long HEARTBEAT_TIMEOUT_MINUTES = 15;

    private final SyncProperties syncProperties;
    private final ApiHealthRecorder recorder;
    private final AtomicReference<Instant> lastHeartbeat = new AtomicReference<>(Instant.now());

    public SentryHealthController(SyncProperties syncProperties,
                                  ApiHealthRecorder recorder) {
        this.syncProperties = syncProperties;
        this.recorder = recorder;
    }

    /**
     * Retourne l'état de la connexion Sentry.
     * <p>
     * {@code 200 UP} — DSN configuré, heartbeat récent.<br>
     * {@code 503 DOWN} — DSN absent ou heartbeat trop vieux (&gt; 15 min).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String dsn = syncProperties.getAlert().getSentryDsn();
        boolean dsnConfigured = dsn != null && !dsn.isBlank();
        Instant last = lastHeartbeat.get();
        Instant timeout = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_MINUTES * 60);
        boolean heartbeatRecent = last.isAfter(timeout);

        Map<String, Object> body = Map.of(
                "status", heartbeatRecent && dsnConfigured ? "UP" : "DOWN",
                "sentry.status", heartbeatRecent ? "HEARTBEAT_OK" : "HEARTBEAT_STALE",
                "sentry.dsn.configured", dsnConfigured,
                "sentry.environment", syncProperties.getAlert().getSentryEnvironment(),
                "sentry.heartbeat.last_sent", last.toString(),
                "sentry.heartbeat.timeout_minutes", HEARTBEAT_TIMEOUT_MINUTES
        );

        if (!dsnConfigured) {
            body = Map.of(
                    "status", "DOWN",
                    "sentry.status", "DSN_NOT_CONFIGURED",
                    "sentry.heartbeat.last_sent", last.toString()
            );
        }

        boolean up = heartbeatRecent && dsnConfigured;
        return ResponseEntity.status(up ? 200 : 503).body(body);
    }

    /**
     * Envoie un heartbeat event à Sentry et met à jour le timestamp interne.
     * Appelé par {@link SyncHealthMonitor} à chaque snapshot.
     */
    public void sendHeartbeat() {
        long t0 = System.currentTimeMillis();
        try {
            SentryEvent event = new SentryEvent();
            event.setLevel(SentryLevel.INFO);
            Message message = new Message();
            message.setFormatted("[SENTRY HEARTBEAT] Backend alive — sync monitor running");
            event.setMessage(message);
            event.setTag("heartbeat", "true");
            event.setTag("service", "lmp-erp-sync");
            event.setFingerprints(java.util.List.of("sentry-heartbeat"));
            Sentry.captureEvent(event);
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Sentry", latency, true, null);
            lastHeartbeat.set(Instant.now());
            log.debug("💓 [SENTRY] Heartbeat sent");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Sentry", latency, false, e.getMessage());
            log.warn("❌ [SENTRY] Heartbeat failed: {}", e.getMessage());
        }
    }
}
