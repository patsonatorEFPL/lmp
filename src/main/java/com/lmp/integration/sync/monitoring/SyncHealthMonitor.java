package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncDirection;
import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.SyncStatus;
import com.lmp.integration.sync.repository.SyncEventRepository;
import com.lmp.integration.sync.service.SyncReconciliationService;
import com.lmp.integration.sync.verification.SyncVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Moniteur périodique de l'état de santé de la synchronisation ERP.
 * <p>
 * Exécuté toutes les 5 minutes par défaut. Collecte les métriques,
 * persiste un snapshot, et déclenche les alertes si les seuils sont dépassés.
 */
@Service
public class SyncHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(SyncHealthMonitor.class);

    private final SyncEventRepository syncEventRepository;
    private final SyncHealthSnapshotRepository snapshotRepository;
    private final SyncErrorPatternRepository patternRepository;
    private final SyncAlertingService alertingService;
    private final ExternalSystemClient externalClient;
    private final SyncVerificationService verificationService;
    private final SyncProperties syncProperties;
    private final SentryHealthController sentryHealthController;

    private Instant lastRun = Instant.now().minusSeconds(3600);

    public SyncHealthMonitor(SyncEventRepository syncEventRepository,
                             SyncHealthSnapshotRepository snapshotRepository,
                             SyncErrorPatternRepository patternRepository,
                             SyncAlertingService alertingService,
                             ExternalSystemClient externalClient,
                             SyncVerificationService verificationService,
                             SyncProperties syncProperties,
                             SentryHealthController sentryHealthController) {
        this.syncEventRepository = syncEventRepository;
        this.snapshotRepository = snapshotRepository;
        this.patternRepository = patternRepository;
        this.alertingService = alertingService;
        this.externalClient = externalClient;
        this.verificationService = verificationService;
        this.syncProperties = syncProperties;
        this.sentryHealthController = sentryHealthController;
    }

    /**
     * Cycle de monitoring toutes les 5 minutes.
     */
    @Scheduled(fixedDelayString = "${lmp.sync.monitor.interval-ms:300000}")
    @Transactional
    public void runHealthCheck() {
        if (!syncProperties.isEnabled()) {
            return;
        }

        Instant now = Instant.now();
        log.debug("🔍 [SYNC MONITOR] Running health check...");

        SyncHealthSnapshot snapshot = new SyncHealthSnapshot();
        snapshot.setRecordedAt(now);

        // 1. Queue depth
        long queueDepth = syncEventRepository.countByStatusIn(
                List.of(SyncStatus.QUEUED, SyncStatus.PROCESSING));
        snapshot.setQueueDepth((int) queueDepth);

        // 2. DEAD events since last run
        long deadCount = syncEventRepository.countByStatusAndCreatedAtAfter(
                SyncStatus.DEAD, LocalDateTime.ofInstant(lastRun, java.time.ZoneOffset.UTC));
        snapshot.setDeadCount((int) deadCount);

        // 3. FAILED stale (> 15 min)
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(15);
        long failedStale = syncEventRepository.countByStatusAndCreatedAtAfter(
                SyncStatus.FAILED, staleThreshold);
        snapshot.setFailedStaleCount((int) failedStale);

        // 4. Unverified SUCCESS > 1h
        LocalDateTime unverifiedThreshold = LocalDateTime.now().minusHours(1);
        long unverified = verificationService.countStaleUnverifiedEvents(unverifiedThreshold);
        snapshot.setUnverifiedCount((int) unverified);

        // 5. Success rate 24h
        BigDecimal successRate = computeSuccessRate24h();
        snapshot.setSuccessRate24h(successRate);

        // 6. ERP availability + latency
        boolean erpAvailable = externalClient.isAvailable();
        snapshot.setErpAvailable(erpAvailable);
        // Latency pourrait être mesurée via un ping dédié — pour l'instant on ne l'a pas

        // 7. New unknown patterns
        long newPatterns = patternRepository.countByCategoryAndAlerted(
                SyncErrorPattern.Category.UNKNOWN, false);
        snapshot.setNewPatternsCount((int) newPatterns);

        // 8. Overall status
        snapshot.setOverallStatus(determineOverallStatus(snapshot));

        // 9. Raw metrics (extensible)
        snapshot.setRawMetrics(Map.of(
                "lastRun", lastRun.toString(),
                "monitorVersion", "1.0"
        ));

        snapshotRepository.save(snapshot);
        log.info("📊 [SYNC MONITOR] Snapshot saved: {} | queue={} dead={} failed_stale={} unverified={} new_patterns={}",
                snapshot.getOverallStatus(), queueDepth, deadCount, failedStale, unverified, newPatterns);

        // 10. Alerting
        if (snapshot.getOverallStatus() != SyncHealthSnapshot.OverallStatus.HEALTHY) {
            alertingService.alertOnSnapshot(snapshot);
        }

        // 11. Alert on unknown patterns
        List<SyncErrorPattern> unalertedUnknown = patternRepository.findUnalertedUnknownPatterns();
        for (SyncErrorPattern pattern : unalertedUnknown) {
            alertingService.alertOnUnknownError(pattern);
        }

        // 12. Sentry heartbeat — confirme que le backend communique avec Sentry
        sentryHealthController.sendHeartbeat();

        lastRun = now;
    }

    private BigDecimal computeSuccessRate24h() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        long total = syncEventRepository.countByDirectionAndCreatedAtAfter(SyncDirection.OUTBOUND, since24h);
        if (total == 0) return BigDecimal.ONE; // 100% si rien n'a été envoyé

        long success = syncEventRepository.countByStatusAndDirectionAndCreatedAtAfter(
                SyncStatus.SUCCESS, SyncDirection.OUTBOUND, since24h);
        return BigDecimal.valueOf(success)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private SyncHealthSnapshot.OverallStatus determineOverallStatus(SyncHealthSnapshot s) {
        var alertCfg = syncProperties.getAlert();

        if (s.getDeadCount() >= alertCfg.getDeadThreshold()
                || s.getNewPatternsCount() > 0) {
            return SyncHealthSnapshot.OverallStatus.CRITICAL;
        }

        if (s.getFailedStaleCount() >= alertCfg.getFailedStaleThreshold()
                || s.getUnverifiedCount() >= alertCfg.getUnverifiedThreshold()
                || !s.isErpAvailable()) {
            return SyncHealthSnapshot.OverallStatus.DEGRADED;
        }

        return SyncHealthSnapshot.OverallStatus.HEALTHY;
    }
}
