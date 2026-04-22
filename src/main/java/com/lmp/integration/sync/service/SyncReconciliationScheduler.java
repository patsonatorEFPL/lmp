package com.lmp.integration.sync.service;

import com.lmp.integration.sync.SyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scheduler de réconciliation avec backoff incrémental.
 * <p>
 * Démarre à 30s d'intervalle. Si aucun écart n'est détecté, l'intervalle
 * augmente de 30s (linéaire) jusqu'à un maximum de 15 minutes.
 * Si un écart est détecté, l'intervalle reset à 30s pour surveiller de plus près.
 * <p>
 * Exemple de progression :
 * <pre>
 *   30s → 1min → 1m30s → 2min → ... → 15min (cap)
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "lmp.sync.reconciliation.enabled", havingValue = "true")
public class SyncReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncReconciliationScheduler.class);

    private final SyncReconciliationService reconciliationService;
    private final SyncProperties syncProperties;

    /** Intervalle actuel en secondes. */
    private final AtomicInteger currentIntervalSeconds;

    /** Prochaine exécution planifiée. */
    private final AtomicReference<Instant> nextRunAt;

    public SyncReconciliationScheduler(SyncReconciliationService reconciliationService,
                                        SyncProperties syncProperties) {
        this.reconciliationService = reconciliationService;
        this.syncProperties = syncProperties;

        int initialInterval = syncProperties.getReconciliation().getInitialIntervalSeconds();
        this.currentIntervalSeconds = new AtomicInteger(initialInterval);
        this.nextRunAt = new AtomicReference<>(Instant.now().plusSeconds(initialInterval));

        log.info("🔍 [RECONCILIATION] Scheduler initialized — initial interval={}s, max={}s, increment={}s",
                initialInterval,
                syncProperties.getReconciliation().getMaxIntervalSeconds(),
                syncProperties.getReconciliation().getIncrementSeconds());
    }

    /**
     * Vérifie chaque seconde si le prochain cycle de réconciliation est dû.
     * L'intervalle réel est géré dynamiquement via {@code nextRunAt}.
     */
    @Scheduled(fixedDelay = 1000)
    public void checkAndReconcile() {
        if (Instant.now().isBefore(nextRunAt.get())) {
            return;
        }

        int currentInterval = currentIntervalSeconds.get();
        log.info("🔍 [RECONCILIATION] Running cycle — current interval={}s", currentInterval);

        try {
            int gaps = reconciliationService.reconcile();

            if (gaps > 0) {
                // Écart détecté → reset à l'intervalle initial
                int initialInterval = syncProperties.getReconciliation().getInitialIntervalSeconds();
                currentIntervalSeconds.set(initialInterval);
                log.info("🔍 [RECONCILIATION] {} gaps found — resetting interval to {}s", gaps, initialInterval);
            } else {
                // Aucun écart → incrémenter l'intervalle
                int increment = syncProperties.getReconciliation().getIncrementSeconds();
                int maxInterval = syncProperties.getReconciliation().getMaxIntervalSeconds();
                int newInterval = Math.min(currentInterval + increment, maxInterval);
                currentIntervalSeconds.set(newInterval);

                if (newInterval != currentInterval) {
                    log.info("🔍 [RECONCILIATION] No gaps — increasing interval to {}s", newInterval);
                }
            }
        } catch (Exception e) {
            log.error("❌ [RECONCILIATION] Cycle failed: {}", e.getMessage(), e);
        }

        // Planifier le prochain cycle
        nextRunAt.set(Instant.now().plusSeconds(currentIntervalSeconds.get()));
    }

    /**
     * Retourne l'intervalle actuel en secondes (pour monitoring/dashboard).
     */
    public int getCurrentIntervalSeconds() {
        return currentIntervalSeconds.get();
    }

    /**
     * Force un reset de l'intervalle à la valeur initiale (pour déclenchement manuel).
     */
    public void resetInterval() {
        int initialInterval = syncProperties.getReconciliation().getInitialIntervalSeconds();
        currentIntervalSeconds.set(initialInterval);
        nextRunAt.set(Instant.now());
        log.info("🔍 [RECONCILIATION] Interval manually reset to {}s", initialInterval);
    }
}
