package com.lmp.integration.sync.service;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.SyncStatus;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.repository.SyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler de retry automatique pour les événements FAILED.
 * <p>
 * Toutes les 60 secondes (configurable), récupère les événements FAILED retryables
 * (retry_count < max_retries, scheduled_at passé) et les requeue en statut QUEUED
 * avec un backoff exponentiel : délai = base × 2^(retry_count).
 * <p>
 * Les événements ayant atteint le max de retries sont marqués DEAD.
 * Utilise {@code FOR UPDATE SKIP LOCKED} pour le traitement concurrent.
 */
@Component
@ConditionalOnProperty(name = "lmp.sync.enabled", havingValue = "true")
public class SyncRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncRetryScheduler.class);

    private final SyncEventRepository syncEventRepository;
    private final SyncProperties syncProperties;

    public SyncRetryScheduler(SyncEventRepository syncEventRepository,
                              SyncProperties syncProperties) {
        this.syncEventRepository = syncEventRepository;
        this.syncProperties = syncProperties;
    }

    /**
     * Requeue les événements FAILED retryables avec backoff exponentiel.
     */
    @Scheduled(fixedDelayString = "${lmp.sync.retry.delay-seconds:60}000")
    @Transactional
    public void retryFailedEvents() {
        int batchSize = syncProperties.getQueue().getBatchSize();
        List<SyncEvent> failedEvents = syncEventRepository.findRetryableFailed(batchSize);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("🔁 [SYNC RETRY] Found {} failed events to process", failedEvents.size());

        int requeued = 0;
        int dead = 0;

        for (SyncEvent event : failedEvents) {
            if (event.getRetryCount() >= event.getMaxRetries()) {
                // Max retries atteint → marquer DEAD
                event.setStatus(SyncStatus.DEAD);
                event.setProcessedAt(LocalDateTime.now());
                syncEventRepository.save(event);
                dead++;
                log.error("💀 [SYNC RETRY] Event {} ({} {}) — max retries ({}) reached — DEAD",
                        event.getId(), event.getEntityType(), event.getEventType(), event.getMaxRetries());
            } else {
                // Requeue avec backoff exponentiel
                event.setStatus(SyncStatus.QUEUED);
                long backoffSeconds = syncProperties.getRetry().getDelaySeconds()
                        * (long) Math.pow(2, event.getRetryCount());
                event.setScheduledAt(LocalDateTime.now().plusSeconds(backoffSeconds));
                event.setErrorMessage(null);
                syncEventRepository.save(event);
                requeued++;
                log.info("🔁 [SYNC RETRY] Requeued event {} ({} {}) — retry {}/{} — next in {}s",
                        event.getId(), event.getEntityType(), event.getEventType(),
                        event.getRetryCount() + 1, event.getMaxRetries(), backoffSeconds);
            }
        }

        log.info("🔁 [SYNC RETRY] Batch complete — {} requeued, {} dead", requeued, dead);
    }
}
