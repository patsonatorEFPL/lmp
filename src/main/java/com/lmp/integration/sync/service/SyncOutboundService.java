package com.lmp.integration.sync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.integration.sync.*;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.repository.SyncEventRepository;
import com.lmp.integration.sync.verification.SyncVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.MDC;

import io.sentry.Sentry;

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
    private final SyncVerificationService verificationService;
    private final ObjectMapper objectMapper;

    private final com.lmp.integration.sync.monitoring.SyncErrorClassifier errorClassifier;
    private final com.lmp.integration.sync.monitoring.SyncMetricsService metricsService;

    public SyncOutboundService(ExternalSystemClient externalClient,
                               SyncEventRepository syncEventRepository,
                               SyncProperties syncProperties,
                               SyncCallbackService syncCallbackService,
                               @Lazy SyncVerificationService verificationService,
                               ObjectMapper objectMapper,
                               com.lmp.integration.sync.monitoring.SyncErrorClassifier errorClassifier,
                               com.lmp.integration.sync.monitoring.SyncMetricsService metricsService) {
        this.externalClient = externalClient;
        this.syncEventRepository = syncEventRepository;
        this.syncProperties = syncProperties;
        this.syncCallbackService = syncCallbackService;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
        this.errorClassifier = errorClassifier;
        this.metricsService = metricsService;
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
        // L'événement arrive déjà en statut PROCESSING (claimé atomiquement par
        // claimNextBatch). Re-load pour rattacher à la tx courante.
        SyncEvent event = syncEventRepository.findById(syncEvent.getId()).orElse(null);
        if (event == null || event.getStatus() != SyncStatus.PROCESSING) {
            log.debug("⏭️ [SYNC] Event {} not in PROCESSING (status={}) — skipping",
                    syncEvent.getId(), event == null ? "null" : event.getStatus());
            return;
        }

        // Observabilité : propager le correlationId dans les logs et headers REST
        MDC.put("correlationId", event.getId().toString());
        try {
            Map<String, Object> data = deserializePayload(event.getPayload());
            injectIdempotencyKey(data, event);
            ExternalResponse response = executeSync(
                    event.getEntityType(), event.getEventType(),
                    event.getExternalEntityId(), data);
            handleResponse(event, response);
        } catch (Exception e) {
            handleFailure(event, e);
        } finally {
            MDC.remove("correlationId");
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

        MDC.put("correlationId", event.getId().toString());
        try {
            Map<String, Object> data = deserializePayload(event.getPayload());
            injectIdempotencyKey(data, event);
            ExternalResponse response = executeSync(
                    event.getEntityType(), event.getEventType(),
                    event.getExternalEntityId(), data);
            handleResponse(event, response);
        } catch (Exception e) {
            handleFailure(event, e);
        } finally {
            MDC.remove("correlationId");
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
            metricsService.recordEventProcessed(syncEvent.getEntityType(), SyncStatus.SUCCESS);
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

            // Verify-After-Sync — vérification inline (best-effort, non-bloquante)
            try {
                verificationService.verifyAfterSync(syncEvent);
            } catch (Exception e) {
                log.warn("⚠️ [SYNC OUT] Post-sync verification failed for {} {}: {}",
                        syncEvent.getEntityType(), syncEvent.getExternalEntityId(), e.getMessage());
            }
        } else {
            syncEvent.setStatus(SyncStatus.FAILED);
            syncEvent.setErrorMessage(response.errorMessage());
            syncEvent.setRetryCount(syncEvent.getRetryCount() + 1);

            // Si max retries atteint → DEAD
            if (syncEvent.getRetryCount() >= syncEvent.getMaxRetries()) {
                syncEvent.setStatus(SyncStatus.DEAD);
                metricsService.recordEventProcessed(syncEvent.getEntityType(), SyncStatus.DEAD);
                metricsService.recordEventDead(syncEvent.getEntityType());
                log.error("💀 [SYNC OUT] {} {} — max retries reached ({}) — marking DEAD",
                        syncEvent.getEntityType(), syncEvent.getEventType(), syncEvent.getMaxRetries());
            } else {
                metricsService.recordEventProcessed(syncEvent.getEntityType(), SyncStatus.FAILED);
                // Calculer le prochain scheduled_at avec backoff exponentiel
                long backoffSeconds = syncProperties.getRetry().getDelaySeconds()
                        * (long) Math.pow(2, syncEvent.getRetryCount() - 1);
                syncEvent.setScheduledAt(LocalDateTime.now().plusSeconds(backoffSeconds));
                log.warn("⚠️ [SYNC OUT] {} {} failed (retry {}/{}) — next retry in {}s",
                        syncEvent.getEntityType(), syncEvent.getEventType(),
                        syncEvent.getRetryCount(), syncEvent.getMaxRetries(), backoffSeconds);
            }

            // Classification de l'erreur pour détection de drift
            try {
                errorClassifier.classify(syncEvent.getErrorMessage(), syncEvent.getId());
            } catch (Exception ex) {
                log.debug("🔇 [SYNC OUT] Error classification failed: {}", ex.getMessage());
            }
        }
    }

    private void handleFailure(SyncEvent syncEvent, Exception e) {
        syncEvent.setRetryCount(syncEvent.getRetryCount() + 1);
        syncEvent.setErrorMessage(e.getMessage());

        boolean isDead = syncEvent.getRetryCount() >= syncEvent.getMaxRetries();
        if (isDead) {
            syncEvent.setStatus(SyncStatus.DEAD);
            metricsService.recordEventProcessed(syncEvent.getEntityType(), SyncStatus.DEAD);
            metricsService.recordEventDead(syncEvent.getEntityType());
            log.error("💀 [SYNC OUT] Exception during {} {} — max retries — DEAD: {}",
                    syncEvent.getEntityType(), syncEvent.getEventType(), e.getMessage(), e);
        } else {
            syncEvent.setStatus(SyncStatus.FAILED);
            metricsService.recordEventProcessed(syncEvent.getEntityType(), SyncStatus.FAILED);
            long backoffSeconds = syncProperties.getRetry().getDelaySeconds()
                    * (long) Math.pow(2, syncEvent.getRetryCount() - 1);
            syncEvent.setScheduledAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            log.error("❌ [SYNC OUT] Exception during {} {} (retry {}/{}) — next in {}s: {}",
                    syncEvent.getEntityType(), syncEvent.getEventType(),
                    syncEvent.getRetryCount(), syncEvent.getMaxRetries(), backoffSeconds, e.getMessage(), e);
        }

        // Envoi à Sentry — toutes les erreurs de sync outbound avec contexte riche
        try {
            Sentry.withScope(scope -> {
                scope.setTag("sync.direction", "OUTBOUND");
                scope.setTag("sync.entity_type", syncEvent.getEntityType().name());
                scope.setTag("sync.event_type", syncEvent.getEventType());
                scope.setTag("sync.status", isDead ? "DEAD" : "FAILED");
                scope.setTag("sync.retry_count", String.valueOf(syncEvent.getRetryCount()));
                scope.setTag("sync.max_retries", String.valueOf(syncEvent.getMaxRetries()));
                scope.setContexts("sync_event", Map.of(
                        "eventId", syncEvent.getId().toString(),
                        "localEntityId", syncEvent.getLocalEntityId() != null ? syncEvent.getLocalEntityId().toString() : "null",
                        "externalEntityId", syncEvent.getExternalEntityId() != null ? syncEvent.getExternalEntityId() : "null",
                        "errorMessage", syncEvent.getErrorMessage() != null ? syncEvent.getErrorMessage() : "null"
                ));
                Sentry.captureException(e);
            });
        } catch (Exception sentryEx) {
            log.debug("🔇 [SYNC OUT] Sentry capture failed: {}", sentryEx.getMessage());
        }

        // Classification de l'erreur pour détection de drift
        try {
            errorClassifier.classify(syncEvent.getErrorMessage(), syncEvent.getId());
        } catch (Exception ex) {
            log.debug("🔇 [SYNC OUT] Error classification failed: {}", ex.getMessage());
        }
    }

    /**
     * Injecte la clé d'idempotence dans le payload pour les opérations CREATE.
     * Utilise l'UUID du SyncEvent comme clé unique — si un retry reposte le même
     * payload, le système externe peut détecter le doublon via ce champ.
     * <p>
     * Le champ {@code lmp_idempotency_key} doit exister comme custom field sur
     * les DocTypes ERPNext synchronisés (Sales Order, Sales Invoice, Payment Entry, etc.).
     */
    private void injectIdempotencyKey(Map<String, Object> data, SyncEvent event) {
        if ("CREATED".equals(event.getEventType()) && event.getId() != null) {
            data.put("lmp_idempotency_key", event.getId().toString());
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
