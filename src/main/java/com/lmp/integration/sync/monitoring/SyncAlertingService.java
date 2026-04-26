package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.notification.service.NotificationService;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moteur d'alerting pour la synchronisation ERP.
 * <p>
 * Déclenché par :
 * - {@link SyncHealthMonitor} : alertes périodiques basées sur les seuils
 * - {@link SyncErrorClassifier} : alerte immédiate sur erreur inconnue (drift)
 *
 * Canaux supportés : email admin, webhook générique, notification in-app.
 * Cooldown configurable par type d'alerte pour éviter le spam.
 */
@Service
public class SyncAlertingService {

    private static final Logger log = LoggerFactory.getLogger(SyncAlertingService.class);

    private final SyncProperties syncProperties;
    private final NotificationService notificationService;
    private final RestClient restClient;

    /** Dernière alerte envoyée par type (cooldown). */
    private final Map<String, Instant> lastAlertByType = new ConcurrentHashMap<>();

    public SyncAlertingService(SyncProperties syncProperties,
                               NotificationService notificationService) {
        this.syncProperties = syncProperties;
        this.notificationService = notificationService;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Alerte sur un snapshot de santé dégradé/critique.
     * Appelé par le monitor périodique.
     */
    @Async
    public void alertOnSnapshot(SyncHealthSnapshot snapshot) {
        if (!syncProperties.getAlert().isEnabled()) return;

        var alertCfg = syncProperties.getAlert();

        if (snapshot.getDeadCount() >= alertCfg.getDeadThreshold()) {
            maybeSendAlert("DEAD_EVENTS",
                    "🚨 Synchronisation ERP — Events DEAR",
                    String.format("""
                            %d événement(s) de synchronisation sont en état DEAD (échec permanent).
                            
                            Dernier snapshot : %s
                            Queue depth : %d
                            FAILED stale : %d
                            Unverified : %d
                            ERP disponible : %s
                            """,
                            snapshot.getDeadCount(),
                            snapshot.getRecordedAt(),
                            snapshot.getQueueDepth(),
                            snapshot.getFailedStaleCount(),
                            snapshot.getUnverifiedCount(),
                            snapshot.isErpAvailable() ? "OUI" : "NON"));
        }

        if (snapshot.getFailedStaleCount() >= alertCfg.getFailedStaleThreshold()) {
            maybeSendAlert("FAILED_STALE",
                    "⚠️ Synchronisation ERP — Events FAILED bloqués",
                    String.format("""
                            %d événement(s) FAILED n'ont pas été retryés depuis > 15 min.
                            
                            Dernier snapshot : %s
                            Queue depth : %d
                            DEAD : %d
                            """,
                            snapshot.getFailedStaleCount(),
                            snapshot.getRecordedAt(),
                            snapshot.getQueueDepth(),
                            snapshot.getDeadCount()));
        }

        if (snapshot.getUnverifiedCount() >= alertCfg.getUnverifiedThreshold()) {
            maybeSendAlert("UNVERIFIED",
                    "⚠️ Synchronisation ERP — Events non vérifiés",
                    String.format("""
                            %d événement(s) SUCCESS n'ont pas été vérifiés côté ERP depuis > 1h.
                            
                            Dernier snapshot : %s
                            """,
                            snapshot.getUnverifiedCount(),
                            snapshot.getRecordedAt()));
        }
    }

    /**
     * Alerte immédiate sur une erreur inconnue (drift potentiel).
     * Appelé par le classifier en temps réel.
     */
    @Async
    public void alertOnUnknownError(SyncErrorPattern pattern) {
        if (!syncProperties.getAlert().isEnabled()) return;
        if (!syncProperties.getAlert().isUnknownErrorAlert()) return;

        maybeSendAlert("UNKNOWN_ERROR_" + pattern.getId(),
                "🚨 DRIFT ERP détecté — Erreur inconnue",
                String.format("""
                        Une nouvelle erreur de synchronisation a été classifiée comme INCONNUE.
                        Cela indique probablement un changement côté ERPNext (nouveau champ, nouvelle validation, mise à jour).
                        
                        Pattern : %s
                        Première occurrence : %s
                        Dernière occurrence : %s
                        Occurrences : %d
                        
                        Action recommandée : vérifier les logs et la compatibilité ERP.
                        """,
                        pattern.getPattern(),
                        pattern.getFirstSeenAt(),
                        pattern.getLastSeenAt(),
                        pattern.getOccurrences()));

        pattern.setAlerted(true);
    }

    // --- Private helpers ---

    private void maybeSendAlert(String alertType, String subject, String body) {
        var alertCfg = syncProperties.getAlert();
        int cooldownSeconds = alertCfg.getCooldownMinutes() * 60;

        Instant lastAlert = lastAlertByType.get(alertType);
        Instant now = Instant.now();
        if (lastAlert != null && now.isBefore(lastAlert.plusSeconds(cooldownSeconds))) {
            log.debug("🔕 [SYNC ALERT] Cooldown active for '{}' — skipping", alertType);
            return;
        }

        lastAlertByType.put(alertType, now);

        // 1. Email
        try {
            notificationService.sendAdminAlert(alertCfg.getAdminEmail(), subject, body);
            log.info("📧 [SYNC ALERT] Email sent for '{}' to {}", alertType, alertCfg.getAdminEmail());
        } catch (Exception e) {
            log.error("❌ [SYNC ALERT] Failed to send email: {}", e.getMessage());
        }

        // 2. Webhook
        if (!alertCfg.getWebhookUrl().isBlank()) {
            try {
                sendWebhook(alertCfg.getWebhookUrl(), alertType, subject, body);
                log.info("🌐 [SYNC ALERT] Webhook sent for '{}'", alertType);
            } catch (Exception e) {
                log.error("❌ [SYNC ALERT] Failed to send webhook: {}", e.getMessage());
            }
        }

        // 3. Sentry — grouping intelligent, pas de spam
        if (!alertCfg.getSentryDsn().isBlank()) {
            try {
                sendToSentry(alertType, subject, body);
                log.info("📡 [SYNC ALERT] Sentry event sent for '{}'", alertType);
            } catch (Exception e) {
                log.error("❌ [SYNC ALERT] Failed to send to Sentry: {}", e.getMessage());
            }
        }
    }

    private void sendWebhook(String url, String alertType, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "source", "lmp-sync-monitor",
                "alertType", alertType,
                "subject", subject,
                "body", body,
                "timestamp", Instant.now().toString(),
                "severity", alertType.startsWith("UNKNOWN") ? "critical" : "warning"
        );

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Envoie un event Sentry avec tags et contexte riches.
     * Sentry groupe automatiquement par fingerprint + stack trace,
     * évitant l'"alert fatigue" des emails directs.
     */
    private void sendToSentry(String alertType, String subject, String body) {
        SentryEvent event = new SentryEvent();
        event.setLevel(alertType.startsWith("UNKNOWN") ? SentryLevel.FATAL : SentryLevel.WARNING);

        Message message = new Message();
        message.setFormatted(subject);
        message.setMessage(body.length() > 200 ? body.substring(0, 200) + "..." : body);
        event.setMessage(message);

        // Tags pour filtrage et dashboard
        event.setTag("alert.type", alertType);
        event.setTag("alert.source", "sync-monitor");
        event.setTag("service", "lmp-erp-sync");

        // Fingerprint pour grouping intelligent — même pattern = même Issue
        event.setFingerprints(java.util.List.of("sync-alert", alertType));

        Sentry.captureEvent(event);
    }
}
