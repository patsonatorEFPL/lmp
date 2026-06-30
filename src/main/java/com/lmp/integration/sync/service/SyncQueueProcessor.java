package com.lmp.integration.sync.service;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.repository.SyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Processeur FIFO de la queue de synchronisation.
 * <p>
 * Poll la table {@code sync_event_log} toutes les 5 secondes (configurable),
 * récupère un batch d'événements QUEUED triés par {@code created_at} ASC,
 * et les exécute via {@code SyncOutboundService.processEvent()}.
 * <p>
 * Utilise {@code SELECT ... FOR UPDATE SKIP LOCKED} pour permettre
 * le traitement concurrent sans conflit de verrous.
 */
@Component
@ConditionalOnProperty(name = "lmp.sync.enabled", havingValue = "true")
public class SyncQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(SyncQueueProcessor.class);

    private final SyncEventRepository syncEventRepository;
    private final SyncOutboundService syncOutboundService;
    private final SyncProperties syncProperties;
    private final com.lmp.integration.sync.monitoring.SyncMetricsService metricsService;

    public SyncQueueProcessor(SyncEventRepository syncEventRepository,
                              SyncOutboundService syncOutboundService,
                              SyncProperties syncProperties,
                              com.lmp.integration.sync.monitoring.SyncMetricsService metricsService) {
        this.syncEventRepository = syncEventRepository;
        this.syncOutboundService = syncOutboundService;
        this.syncProperties = syncProperties;
        this.metricsService = metricsService;
    }

    /**
     * Poll et claim atomiquement le prochain batch d'événements QUEUED (FIFO).
     * <p>
     * {@code claimNextBatch()} fait un UPDATE...RETURNING qui flip QUEUED→PROCESSING
     * et retourne les rows claimed dans une seule opération atomique. Les autres
     * réplicas voient PROCESSING et n'incluent plus ces rows dans leur prochain
     * SELECT WHERE status='QUEUED' — fini les races multi-replica.
     * <p>
     * processEvent() est REQUIRES_NEW (sa propre tx, sa propre connexion) — pas
     * de contention de verrous avec la tx du claim, qui est commit avant l'appel.
     */
    @Scheduled(fixedDelayString = "${lmp.sync.queue.poll-interval-ms:5000}")
    public void processNextBatch() {
        int batchSize = syncProperties.getQueue().getBatchSize();
        List<SyncEvent> batch = syncEventRepository.claimNextBatch(batchSize);

        long queueDepth = syncEventRepository.countByStatus(
                com.lmp.integration.sync.SyncStatus.QUEUED);
        metricsService.setQueueDepth((int) queueDepth);

        if (batch.isEmpty()) {
            return;
        }

        log.info("[SYNC QUEUE] Processing batch of {} events", batch.size());

        for (SyncEvent event : batch) {
            try {
                syncOutboundService.processEvent(event);
            } catch (Exception e) {
                log.error("[SYNC QUEUE] Unexpected error processing event {}: {}",
                        event.getId(), e.getMessage(), e);
            }
        }

        log.info("[SYNC QUEUE] Batch complete — {} events processed", batch.size());
    }
}
