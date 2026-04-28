package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Cache Caffeine pour les agrégats ERP (totaux, comptages).
 * <p>
 * Évite de surcharger l'ERP avec des appels répétés pour la réconciliation
 * ou le monitoring. TTL = 60s, max-size = 500 (configuré dans
 * {@code application.properties}).
 */
@Service
public class ErpAggregateCacheService {

    private static final Logger log = LoggerFactory.getLogger(ErpAggregateCacheService.class);

    private final ExternalSystemClient externalClient;

    public ErpAggregateCacheService(ExternalSystemClient externalClient) {
        this.externalClient = externalClient;
    }

    /**
     * Récupère un total agrégé côté ERP avec cache 60s.
     */
    @Cacheable(value = "erp-aggregates", key = "#type.name() + ':' + #sumField + ':' + #dateField + ':' + #startDate + ':' + #endDate")
    public BigDecimal fetchAggregatedTotal(SyncEntityType type, String sumField,
                                            String dateField, String startDate, String endDate) {
        log.debug("📊 [ERP-CACHE] Fetching aggregate for {}.{} ({} → {})", type, sumField, startDate, endDate);
        return externalClient.fetchAggregatedTotal(type, sumField, dateField, startDate, endDate);
    }

    /**
     * Invalide manuellement tout le cache des agrégats ERP.
     * Peut être déclenché depuis un endpoint admin.
     */
    @CacheEvict(value = "erp-aggregates", allEntries = true)
    public void evictAll() {
        log.info("📊 [ERP-CACHE] All entries evicted");
    }
}
