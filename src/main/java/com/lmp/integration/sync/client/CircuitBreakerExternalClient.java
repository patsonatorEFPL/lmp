package com.lmp.integration.sync.client;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Décorateur Resilience4j autour du client REST externe.
 * <p>
 * Ajoute un circuit breaker sur tous les appels ERP. Si le circuit est ouvert,
 * les fallbacks retournent immédiatement une réponse {@code UNAVAILABLE} sans
 * appeler le réseau.
 */
@Component
@Primary
@ConditionalOnProperty(name = "lmp.sync.enabled", havingValue = "true")
public class CircuitBreakerExternalClient implements ExternalSystemClient {

    private final ExternalSystemClient delegate;

    public CircuitBreakerExternalClient(@Qualifier("restExternalClient") ExternalSystemClient delegate) {
        this.delegate = delegate;
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackBoolean")
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackResponse")
    public ExternalResponse createEntity(SyncEntityType type, Map<String, Object> data) {
        ExternalResponse r = delegate.createEntity(type, data);
        if (!r.success()) {
            throw new ExternalSystemCallException(r.errorMessage());
        }
        return r;
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackResponse")
    public ExternalResponse updateEntity(SyncEntityType type, String externalId, Map<String, Object> data) {
        ExternalResponse r = delegate.updateEntity(type, externalId, data);
        if (!r.success()) {
            throw new ExternalSystemCallException(r.errorMessage());
        }
        return r;
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackResponse")
    public ExternalResponse getEntity(SyncEntityType type, String externalId) {
        ExternalResponse r = delegate.getEntity(type, externalId);
        if (!r.success()) {
            throw new ExternalSystemCallException(r.errorMessage());
        }
        return r;
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackResponse")
    public ExternalResponse deleteEntity(SyncEntityType type, String externalId) {
        ExternalResponse r = delegate.deleteEntity(type, externalId);
        if (!r.success()) {
            throw new ExternalSystemCallException(r.errorMessage());
        }
        return r;
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackListString")
    public List<String> listLinkedEntityIds(SyncEntityType type, String parentField, String parentId) {
        return delegate.listLinkedEntityIds(type, parentField, parentId);
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackListMap")
    public List<Map<String, Object>> listEntities(SyncEntityType type, Instant modifiedSince) {
        return delegate.listEntities(type, modifiedSince);
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackBigDecimal")
    public BigDecimal fetchAggregatedTotal(SyncEntityType type, String sumField, String dateField,
                                            String startDate, String endDate) {
        return delegate.fetchAggregatedTotal(type, sumField, dateField, startDate, endDate);
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackResponse")
    public ExternalResponse callMethod(String method, Map<String, Object> args) {
        ExternalResponse r = delegate.callMethod(method, args);
        if (!r.success()) {
            throw new ExternalSystemCallException(r.errorMessage());
        }
        return r;
    }

    @Override
    @CircuitBreaker(name = "erp", fallbackMethod = "fallbackOptionalMap")
    public Optional<Map<String, Object>> findFirstByFilters(SyncEntityType type, String filterJson) {
        return delegate.findFirstByFilters(type, filterJson);
    }

    // ==================== Fallbacks ====================

    @SuppressWarnings("unused")
    private ExternalResponse fallbackResponse(SyncEntityType type, Map<String, Object> data, Throwable t) {
        return ExternalResponse.unavailable();
    }

    @SuppressWarnings("unused")
    private ExternalResponse fallbackResponse(SyncEntityType type, String externalId, Map<String, Object> data, Throwable t) {
        return ExternalResponse.unavailable();
    }

    @SuppressWarnings("unused")
    private ExternalResponse fallbackResponse(SyncEntityType type, String externalId, Throwable t) {
        return ExternalResponse.unavailable();
    }

    @SuppressWarnings("unused")
    private boolean fallbackBoolean(Throwable t) {
        return false;
    }

    @SuppressWarnings("unused")
    private List<String> fallbackListString(SyncEntityType type, String parentField, String parentId, Throwable t) {
        return List.of();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> fallbackListMap(SyncEntityType type, Instant modifiedSince, Throwable t) {
        return List.of();
    }

    @SuppressWarnings("unused")
    private BigDecimal fallbackBigDecimal(SyncEntityType type, String sumField, String dateField,
                                          String startDate, String endDate, Throwable t) {
        return null;
    }

    @SuppressWarnings("unused")
    private ExternalResponse fallbackMethod(String method, Map<String, Object> args, Throwable t) {
        return ExternalResponse.unavailable();
    }

    @SuppressWarnings("unused")
    private Optional<Map<String, Object>> fallbackOptionalMap(SyncEntityType type, String filterJson, Throwable t) {
        return Optional.empty();
    }

    /**
     * Exception interne signalant un échec d'appel au système externe.
     * Permet au circuit breaker de compter les erreurs.
     */
    private static class ExternalSystemCallException extends RuntimeException {
        ExternalSystemCallException(String message) {
            super(message);
        }
    }
}
