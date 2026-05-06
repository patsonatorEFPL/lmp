package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.SyncProperties;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health indicator Spring Boot pour Sentry.
 * <p>
 * Expose l'état de la connexion Sentry sur {@code /actuator/health/sentry} :
 * <ul>
 *   <li><b>UP</b> : DSN configuré, SDK initialisé, heartbeat récent (&lt; 15 min)</li>
 *   <li><b>DOWN</b> : DSN absent ou heartbeat trop vieux</li>
 * </ul>
 * <p>
 * Le heartbeat est envoyé automatiquement par {@link SyncHealthMonitor} à chaque
 * snapshot (toutes les 5 min). Si Sentry ne reçoit rien depuis &gt; 15 min,
 * c'est probablement un problème réseau ou de DSN.
 */
@Component
public class SentryHealthIndicator implements HealthIndicator {

    private final SyncProperties syncProperties;

    /** Timestamp du dernier heartbeat envoyé à Sentry. */
    private final AtomicReference<Instant> lastHeartbeat = new AtomicReference<>(Instant.now());

    private static final long HEARTBEAT_TIMEOUT_MINUTES = 15;

    public SentryHealthIndicator(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    @Override
    public Health health() {
        String dsn = syncProperties.getAlert().getSentryDsn();
        boolean dsnConfigured = dsn != null && !dsn.isBlank();

        if (!dsnConfigured) {
            return Health.down()
                    .withDetail("sentry.status", "DSN_NOT_CONFIGURED")
                    .withDetail("sentry.heartbeat.last_sent", lastHeartbeat.get().toString())
                    .build();
        }

        Instant last = lastHeartbeat.get();
        Instant timeout = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_MINUTES * 60);
        boolean heartbeatRecent = last.isAfter(timeout);

        Health.Builder builder = heartbeatRecent ? Health.up() : Health.down();
        return builder
                .withDetail("sentry.status", heartbeatRecent ? "HEARTBEAT_OK" : "HEARTBEAT_STALE")
                .withDetail("sentry.dsn.configured", true)
                .withDetail("sentry.environment", syncProperties.getAlert().getSentryEnvironment())
                .withDetail("sentry.heartbeat.last_sent", last.toString())
                .withDetail("sentry.heartbeat.timeout_minutes", HEARTBEAT_TIMEOUT_MINUTES)
                .build();
    }

    /**
     * Envoie un heartbeat event à Sentry et met à jour le timestamp interne.
     * Appelé par {@link SyncHealthMonitor} à chaque snapshot.
     */
    public void sendHeartbeat() {
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
            lastHeartbeat.set(Instant.now());
        } catch (Exception e) {
            // Silencieux — ne pas casser le monitor si Sentry est down
        }
    }
}
