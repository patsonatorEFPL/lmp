package com.lmp.shared.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.lmp.shared.geo.GeoResolution;

/**
 * Configuration du cache Caffeine pour la géolocalisation IP.
 *
 * <h3>Stratégie de cache</h3>
 * <ul>
 *   <li><b>Clé</b> : Adresse IP (String)</li>
 *   <li><b>Valeur</b> : {@link GeoResolution} (pays + devise)</li>
 *   <li><b>TTL</b> : Configurable (défaut 15 minutes)</li>
 *   <li><b>Taille max</b> : Configurable (défaut 10 000 entrées)</li>
 *   <li><b>Éviction</b> : LRU (Least Recently Used) automatique</li>
 *   <li><b>Métriques</b> : Statistiques de performance enregistrées et loguées toutes les 5 minutes</li>
 * </ul>
 *
 * <h3>Dimensionnement mémoire</h3>
 * <pre>
 * 10 000 entrées × ~220 bytes/entrée ≈ 2.2 MB
 * 50 000 entrées × ~220 bytes/entrée ≈ 11 MB
 * </pre>
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@code geoip.cache.max-size} : Nombre max d'IPs en cache (défaut : 10000)</li>
 *   <li>{@code geoip.cache.ttl-minutes} : Durée de vie en minutes (défaut : 15)</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class GeoIpCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(GeoIpCacheConfig.class);

    private Cache<String, GeoResolution> geoIpCache;

    @Bean
    public Cache<String, GeoResolution> geoIpCache(
            @Value("${geoip.cache.max-size:10000}") int maxSize,
            @Value("${geoip.cache.ttl-minutes:15}") int ttlMinutes) {

        logger.info("[GeoIP Cache] Initialisation — max-size: {}, TTL: {} min", maxSize, ttlMinutes);

        this.geoIpCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats() // Active les métriques de performance
                .build();

        return this.geoIpCache;
    }

    /**
     * Log les statistiques du cache toutes les 5 minutes.
     * Permet de monitorer :
     * - Taux de hit/miss
     * - Taille actuelle
     * - Nombre d'évictions (overflow LRU)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logCacheStats() {
        if (geoIpCache == null) {
            return;
        }

        CacheStats stats = geoIpCache.stats();
        long size = geoIpCache.estimatedSize();
        long totalRequests = stats.requestCount();
        
        if (totalRequests == 0) {
            logger.debug("[GeoIP Cache] Aucune requête depuis le dernier log");
            return;
        }

        double hitRate = stats.hitRate() * 100;
        long hits = stats.hitCount();
        long misses = stats.missCount();
        long evictions = stats.evictionCount();

        logger.info("[GeoIP Cache] Stats — Size: {}, Hits: {}, Misses: {}, Hit Rate: {:.1f}%, Evictions: {}",
                size, hits, misses, hitRate, evictions);

        // Alerte si taux de hit faible (< 50%)
        if (totalRequests > 100 && hitRate < 50.0) {
            logger.warn("[GeoIP Cache] Taux de hit faible ({:.1f}%) — Envisager d'augmenter TTL ou max-size", hitRate);
        }

        // Alerte si beaucoup d'évictions (signe que le cache est trop petit)
        if (evictions > size) {
            logger.warn("[GeoIP Cache] Évictions élevées ({}) — Cache trop petit ? max-size actuel: {}", 
                    evictions, geoIpCache.policy().eviction().get().getMaximum());
        }
    }
}
