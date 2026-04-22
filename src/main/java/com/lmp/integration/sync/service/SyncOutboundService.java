package com.lmp.integration.sync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.integration.sync.*;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.repository.SyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les événements sortants (LMP → système externe).
 * <p>
 * Mode queue FIFO : les événements sont enqueués dans {@code sync_event_log}
 * avec statut QUEUED. Le {@code SyncQueueProcessor} les consomme en ordre FIFO.
 */
@Service
public class SyncOutboundService {

    private static final Logger log = LoggerFactory.getLogger(SyncOutboundService.class);

    private final ExternalSystemClient externalClient;
    private final SyncEventRepository syncEventRepository;
    private final SyncProperties syncProperties;
    private final SyncCallbackService syncCallbackService;
    private final ObjectMapper objectMapper;

    public SyncOutboundService(ExternalSystemClient externalClient,
                               SyncEventRepository syncEventRepository,
                               SyncProperties syncProperties,
                               SyncCallbackService syncCallbackService,
                               ObjectMapper objectMapper) {
        this.externalClient = externalClient;
        this.syncEventRepository = syncEventRepository;
        this.syncProperties = syncProperties;
        this.syncCallbackService = syncCallbackService;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueue une entité pour synchronisation vers le système externe (FIFO).
     * L'événement est inséré avec statut QUEUED et sera traité par le SyncQueueProcessor.
     *
     * @param entityType    type d'entité (CUSTOMER, ITEM, etc.)
     * @param eventType     type d'événement (CREATED, UPDATED)
     * @param localEntityId UUID de l'entité locale
     * @param externalId    ID externe existant (pour les mises à jour) — peut être null
     * @param data          payload de l'entité
     */
    @Transactional
    public void enqueue(SyncEntityType entityType, String eventType, UUID localEntityId,
                        String externalId, Map<String, Object> data) {
        if (!syncProperties.isEnabled()) {
            log.debug("🔇 [SYNC] Synchronisation désactivée — événement {} {} ignoré", entityType, eventType);
            return;
        }

        SyncEvent syncEvent = new SyncEvent();
        syncEvent.setDirection(SyncDirection.OUTBOUND);
        syncEvent.setEntityType(entityType);
        syncEvent.setLocalEntityId(localEntityId);
        syncEvent.setExternalEntityId(externalId);
        syncEvent.setEventType(eventType);
        syncEvent.setOperation(eventType);
        syncEvent.setStatus(SyncStatus.QUEUED);
        syncEvent.setPayload(serializePayload(data));
        syncEvent.setMaxRetries(syncProperties.getRetry().getMaxAttempts());
        syncEvent.setScheduledAt(LocalDateTime.now());

        syncEventRepository.save(syncEvent);
        log.info("📥 [SYNC QUEUE] Enqueued {} {} for entity {} — queueId={}",
                entityType, eventType, localEntityId, syncEvent.getId());
    }

    /**
     * Alias de compatibilité — redirige vers enqueue().
     */
    public void syncEntity(SyncEntityType entityType, String eventType, UUID localEntityId,
                           String externalId, Map<String, Object> data) {
        enqueue(entityType, eventType, localEntityId, externalId, data);
    }

    /**
     * Exécute la synchronisation d'un événement (appelé par le SyncQueueProcessor).
     * <p>
     * Utilise REQUIRES_NEW pour isoler chaque événement dans sa propre transaction.
     * Cela garantit que le callback (ex: sauvegarde de l'externalOrderId sur l'Order)
     * est commité même si un événement suivant du batch échoue.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processEvent(SyncEvent syncEvent) {
        // Re-charger l'entité dans cette transaction (l'objet reçu est détaché
        // depuis que processNextBatch n'est plus @Transactional)
        SyncEvent event = syncEventRepository.findById(syncEvent.getId()).orElse(null);
        if (event == null || event.getStatus() != SyncStatus.QUEUED) {
            log.debug("⏭️ [SYNC] Event {} already processed or missing — skipping", syncEvent.getId());
            return;
        }

        event.setStatus(SyncStatus.PROCESSING);
        syncEventRepository.save(event);

        try {
            Map<String, Object> data = deserializePayload(event.getPayload());
            ExternalResponse response = executeSync(
                    event.getEntityType(), event.getEventType(),
                    event.getExternalEntityId(), data);
            handleResponse(event, response);
        } catch (Exception e) {
            handleFailure(event, e);
        }

        syncEventRepository.save(event);
    }

    /**
     * Retente un événement échoué (appelé par le SyncRetryScheduler).
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void retryEvent(SyncEvent syncEvent) {
        SyncEvent event = syncEventRepository.findById(syncEvent.getId()).orElse(null);
        if (event == null) {
            log.debug("⏭️ [SYNC] Retry event {} not found — skipping", syncEvent.getId());
            return;
        }

        event.setStatus(SyncStatus.PROCESSING);
        syncEventRepository.save(event);

        try {
            Map<String, Object> data = deserializePayload(event.getPayload());
            ExternalResponse response = executeSync(
                    event.getEntityType(), event.getEventType(),
                    event.getExternalEntityId(), data);
            handleResponse(event, response);
        } catch (Exception e) {
            handleFailure(event, e);
        }

        syncEventRepository.save(event);
    }

    private ExternalResponse executeSync(SyncEntityType entityType, String eventType,
                                         String externalId, Map<String, Object> data) {
        return switch (eventType) {
            case "CREATED" -> externalClient.createEntity(entityType, data);
            case "UPDATED" -> {
                if (externalId == null || externalId.isBlank()) {
                    log.warn("⚠️ [SYNC] Cannot update {} without externalId — skipping", entityType);
                    yield ExternalResponse.failure("Missing externalId for update", 400);
                }
                yield externalClient.updateEntity(entityType, externalId, data);
            }
            case "DELETED" -> {
                if (externalId == null || externalId.isBlank()) {
                    log.warn("⚠️ [SYNC] Cannot delete {} without externalId — skipping", entityType);
                    yield ExternalResponse.failure("Missing externalId for delete", 400);
                }
                yield externalClient.deleteEntity(entityType, externalId);
            }
            default -> {
                log.warn("⚠️ [SYNC] Unhandled event type: {}", eventType);
                yield ExternalResponse.failure("Unknown event type: " + eventType, 400);
            }
        };
    }

    private void handleResponse(SyncEvent syncEvent, ExternalResponse response) {
        if (response.success()) {
            syncEvent.setStatus(SyncStatus.SUCCESS);
            syncEvent.setExternalEntityId(response.externalId());
            syncEvent.setProcessedAt(LocalDateTime.now());
            log.info("✅ [SYNC OUT] {} {} → externalId={}",
                    syncEvent.getEntityType(), syncEvent.getEventType(), response.externalId());

            // Callback — mettre à jour ou nettoyer l'entité locale
            if (syncEvent.getLocalEntityId() != null) {
                try {
                    if ("DELETED".equals(syncEvent.getEventType())) {
                        syncCallbackService.onDeleteSuccess(
                                syncEvent.getEntityType(), syncEvent.getLocalEntityId(),
                                syncEvent.getExternalEntityId());
                    } else if (response.externalId() != null) {
                        syncCallbackService.onSyncSuccess(
                                syncEvent.getEntityType(), syncEvent.getLocalEntityId(),
                                response.externalId(), response.data());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [SYNC OUT] Callback failed for {} {}: {}",
                            syncEvent.getEntityType(), syncEvent.getLocalEntityId(), e.getMessage());
                }
            }
        } else {
            syncEvent.setStatus(SyncStatus.FAILED);
            syncEvent.setErrorMessage(response.errorMessage());
            syncEvent.setRetryCount(syncEvent.getRetryCount() + 1);

            // Si max retries atteint → DEAD
            if (syncEvent.getRetryCount() >= syncEvent.getMaxRetries()) {
                syncEvent.setStatus(SyncStatus.DEAD);
                log.error("💀 [SYNC OUT] {} {} — max retries reached ({}) — marking DEAD",
                        syncEvent.getEntityType(), syncEvent.getEventType(), syncEvent.getMaxRetries());
            } else {
                // Calculer le prochain scheduled_at avec backoff exponentiel
                long backoffSeconds = syncProperties.getRetry().getDelaySeconds()
                        * (long) Math.pow(2, syncEvent.getRetryCount() - 1);
                syncEvent.setScheduledAt(LocalDateTime.now().plusSeconds(backoffSeconds));
                log.warn("⚠️ [SYNC OUT] {} {} failed (retry {}/{}) — next retry in {}s",
                        syncEvent.getEntityType(), syncEvent.getEventType(),
                        syncEvent.getRetryCount(), syncEvent.getMaxRetries(), backoffSeconds);
            }
        }
    }

    private void handleFailure(SyncEvent syncEvent, Exception e) {
        syncEvent.setRetryCount(syncEvent.getRetryCount() + 1);
        syncEvent.setErrorMessage(e.getMessage());

        if (syncEvent.getRetryCount() >= syncEvent.getMaxRetries()) {
            syncEvent.setStatus(SyncStatus.DEAD);
            log.error("💀 [SYNC OUT] Exception during {} {} — max retries — DEAD: {}",
                    syncEvent.getEntityType(), syncEvent.getEventType(), e.getMessage(), e);
        } else {
            syncEvent.setStatus(SyncStatus.FAILED);
            long backoffSeconds = syncProperties.getRetry().getDelaySeconds()
                    * (long) Math.pow(2, syncEvent.getRetryCount() - 1);
            syncEvent.setScheduledAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            log.error("❌ [SYNC OUT] Exception during {} {} (retry {}/{}) — next in {}s: {}",
                    syncEvent.getEntityType(), syncEvent.getEventType(),
                    syncEvent.getRetryCount(), syncEvent.getMaxRetries(), backoffSeconds, e.getMessage(), e);
        }
    }

    private String serializePayload(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize sync payload: {}", e.getMessage());
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializePayload(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize sync payload: {}", e.getMessage());
            return Map.of();
        }
    }
}
