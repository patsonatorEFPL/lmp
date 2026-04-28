package com.lmp.integration.sync.client;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client no-op — log uniquement. Actif quand {@code lmp.sync.enabled=false} (défaut).
 * <p>
 * Permet au développeur de voir les événements qui seraient envoyés
 * sans nécessiter un système externe opérationnel.
 */
@Component
@ConditionalOnProperty(name = "lmp.sync.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpExternalClient implements ExternalSystemClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpExternalClient.class);

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public ExternalResponse createEntity(SyncEntityType type, Map<String, Object> data) {
        log.info("🔇 [SYNC NO-OP] createEntity({}) — data keys: {}", type, data.keySet());
        return ExternalResponse.unavailable();
    }

    @Override
    public ExternalResponse updateEntity(SyncEntityType type, String externalId, Map<String, Object> data) {
        log.info("🔇 [SYNC NO-OP] updateEntity({}, {}) — data keys: {}", type, externalId, data.keySet());
        return ExternalResponse.unavailable();
    }

    @Override
    public ExternalResponse getEntity(SyncEntityType type, String externalId) {
        log.info("🔇 [SYNC NO-OP] getEntity({}, {})", type, externalId);
        return ExternalResponse.unavailable();
    }

    @Override
    public ExternalResponse deleteEntity(SyncEntityType type, String externalId) {
        log.info("🔇 [SYNC NO-OP] deleteEntity({}, {})", type, externalId);
        return ExternalResponse.unavailable();
    }

    @Override
    public List<String> listLinkedEntityIds(SyncEntityType type, String parentField, String parentId) {
        log.info("🔇 [SYNC NO-OP] listLinkedEntityIds({}, {}={})", type, parentField, parentId);
        return List.of();
    }

    @Override
    public List<Map<String, Object>> listEntities(SyncEntityType type, Instant modifiedSince) {
        log.info("🔇 [SYNC NO-OP] listEntities({}, since={})", type, modifiedSince);
        return List.of();
    }

    @Override
    public java.math.BigDecimal fetchAggregatedTotal(SyncEntityType type, String sumField,
                                                      String dateField, String startDate, String endDate) {
        log.info("🔇 [SYNC NO-OP] fetchAggregatedTotal({}, {}, {}→{})", type, sumField, startDate, endDate);
        return null;
    }

    @Override
    public ExternalResponse callMethod(String method, Map<String, Object> args) {
        log.info("🔇 [SYNC NO-OP] callMethod({}) — args keys: {}", method, args != null ? args.keySet() : "null");
        return ExternalResponse.unavailable();
    }

    @Override
    public Optional<Map<String, Object>> findFirstByFilters(SyncEntityType type, String filterJson) {
        log.info("🔇 [SYNC NO-OP] findFirstByFilters({}, {})", type, filterJson);
        return Optional.empty();
    }
}
