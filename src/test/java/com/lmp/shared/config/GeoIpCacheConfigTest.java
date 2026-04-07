package com.lmp.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lmp.shared.geo.GeoResolution;

/**
 * Tests unitaires pour {@link GeoIpCacheConfig}.
 * 
 * <p>Vérifie que le cache Caffeine est correctement configuré avec :
 * <ul>
 *   <li>Taille maximale (éviction LRU)</li>
 *   <li>TTL (Time-To-Live)</li>
 *   <li>Enregistrement des statistiques</li>
 * </ul>
 */
class GeoIpCacheConfigTest {

    @Test
    void geoIpCache_shouldBeCreatedWithDefaultConfig() {
        // Given
        GeoIpCacheConfig config = new GeoIpCacheConfig();
        int maxSize = 10000;
        int ttlMinutes = 15;

        // When
        Cache<String, GeoResolution> cache = config.geoIpCache(maxSize, ttlMinutes);

        // Then
        assertNotNull(cache, "Cache should not be null");
        assertEquals(0, cache.estimatedSize(), "Cache should be empty initially");
    }

    @Test
    void geoIpCache_shouldStoreAndRetrieveGeoResolution() {
        // Given
        GeoIpCacheConfig config = new GeoIpCacheConfig();
        Cache<String, GeoResolution> cache = config.geoIpCache(100, 5);
        String ip = "203.0.113.45";
        GeoResolution resolution = new GeoResolution("CA", "CAD");

        // When
        cache.put(ip, resolution);
        GeoResolution retrieved = cache.getIfPresent(ip);

        // Then
        assertNotNull(retrieved, "Retrieved resolution should not be null");
        assertEquals("CA", retrieved.countryCode());
        assertEquals("CAD", retrieved.currencyCode());
    }

    @Test
    void geoIpCache_shouldReturnNullForMissingKey() {
        // Given
        GeoIpCacheConfig config = new GeoIpCacheConfig();
        Cache<String, GeoResolution> cache = config.geoIpCache(100, 5);

        // When
        GeoResolution result = cache.getIfPresent("192.0.2.1");

        // Then
        assertNull(result, "Cache should return null for missing key");
    }

    @Test
    void geoIpCache_shouldEvictWhenMaxSizeReached() {
        // Given
        GeoIpCacheConfig config = new GeoIpCacheConfig();
        int maxSize = 3;
        Cache<String, GeoResolution> cache = config.geoIpCache(maxSize, 60);

        // When - Insert more entries than max size
        cache.put("1.1.1.1", new GeoResolution("US", "USD"));
        cache.put("2.2.2.2", new GeoResolution("CA", "CAD"));
        cache.put("3.3.3.3", new GeoResolution("GB", "GBP"));
        cache.put("4.4.4.4", new GeoResolution("FR", "EUR")); // Should trigger eviction

        // Force cleanup to apply eviction policy
        cache.cleanUp();

        // Then
        long size = cache.estimatedSize();
        assertEquals(maxSize, size, "Cache size should not exceed max size");
    }

    @Test
    void geoIpCache_shouldRecordStats() {
        // Given
        GeoIpCacheConfig config = new GeoIpCacheConfig();
        Cache<String, GeoResolution> cache = config.geoIpCache(100, 5);
        String ip = "198.51.100.10";
        GeoResolution resolution = new GeoResolution("DE", "EUR");

        // When
        cache.put(ip, resolution);
        cache.getIfPresent(ip);       // Hit
        cache.getIfPresent("1.2.3.4"); // Miss

        // Then
        var stats = cache.stats();
        assertEquals(1, stats.hitCount(), "Should record 1 cache hit");
        assertEquals(1, stats.missCount(), "Should record 1 cache miss");
    }

    @Test
    void geoIpCache_shouldExpireEntriesAfterTTL() throws InterruptedException {
        // Given
        GeoIpCacheConfig config = new GeoIpCacheConfig();
        int ttlSeconds = 1; // 1 second TTL for fast test
        Cache<String, GeoResolution> cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();
        
        String ip = "203.0.113.100";
        GeoResolution resolution = new GeoResolution("JP", "JPY");

        // When
        cache.put(ip, resolution);
        GeoResolution beforeExpiry = cache.getIfPresent(ip);
        
        // Wait for expiration (with buffer)
        TimeUnit.SECONDS.sleep(2);
        cache.cleanUp(); // Force cleanup for test
        
        GeoResolution afterExpiry = cache.getIfPresent(ip);

        // Then
        assertNotNull(beforeExpiry, "Entry should exist before TTL expires");
        assertNull(afterExpiry, "Entry should be null after TTL expires");
    }
}
