package com.lmp.shared.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Tests unitaires pour {@link GeoCountryLookupService} avec cache Caffeine.
 * 
 * <p>Vérifie :
 * <ul>
 *   <li>Cache HIT : Pas d'appel API quand l'IP est en cache</li>
 *   <li>Cache MISS : Appel API puis mise en cache</li>
 *   <li>Gestion des IPs nulles/vides</li>
 *   <li>Override de test</li>
 * </ul>
 */
class GeoCountryLookupServiceCacheTest {

    private GeoCountryLookupService service;
    private Environment environment;
    private IpWhoIsGeoService ipWhoIsGeoService;
    private IpApiComGeoService ipApiComGeoService;
    private Cache<String, GeoResolution> geoIpCache;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        ipWhoIsGeoService = mock(IpWhoIsGeoService.class);
        ipApiComGeoService = mock(IpApiComGeoService.class);
        
        // Cache réel (pas mock) pour tester le vrai comportement
        geoIpCache = Caffeine.newBuilder()
                .maximumSize(100)
                .recordStats()
                .build();

        service = new GeoCountryLookupService(
                environment,
                "", // No GeoLite2 DB
                ipWhoIsGeoService,
                ipApiComGeoService,
                geoIpCache
        );
    }

    // ── Cache HIT tests ───────────────────────────────────────────────────────

    @Test
    void resolve_cacheMiss_shouldCallApiAndCacheResult() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.45");
        
        GeoResolution apiResponse = new GeoResolution("CA", "CAD");
        when(ipWhoIsGeoService.lookup("203.0.113.45")).thenReturn(Optional.of(apiResponse));

        // When
        Optional<GeoResolution> result1 = service.resolve(request);

        // Then
        assertTrue(result1.isPresent());
        assertEquals("CA", result1.get().countryCode());
        assertEquals("CAD", result1.get().currencyCode());
        
        // Verify API was called
        verify(ipWhoIsGeoService, times(1)).lookup("203.0.113.45");
        
        // Verify result was cached
        GeoResolution cached = geoIpCache.getIfPresent("203.0.113.45");
        assertNotNull(cached);
        assertEquals("CA", cached.countryCode());
    }

    @Test
    void resolve_cacheHit_shouldNotCallApi() {
        // Given
        String ip = "198.51.100.10";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        
        // Pre-populate cache
        GeoResolution cachedResolution = new GeoResolution("DE", "EUR");
        geoIpCache.put(ip, cachedResolution);

        // When
        Optional<GeoResolution> result = service.resolve(request);

        // Then
        assertTrue(result.isPresent());
        assertEquals("DE", result.get().countryCode());
        assertEquals("EUR", result.get().currencyCode());
        
        // Verify API was NOT called (cache hit)
        verify(ipWhoIsGeoService, never()).lookup(anyString());
        verify(ipApiComGeoService, never()).lookup(anyString());
        
        // Verify cache stats
        assertEquals(1, geoIpCache.stats().hitCount());
    }

    @Test
    void resolve_multipleCalls_sameIp_shouldUseCache() {
        // Given
        String ip = "192.0.2.100";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        
        GeoResolution apiResponse = new GeoResolution("FR", "EUR");
        when(ipWhoIsGeoService.lookup(ip)).thenReturn(Optional.of(apiResponse));

        // When - Call 3 times with same IP
        service.resolve(request);
        service.resolve(request);
        service.resolve(request);

        // Then - API should be called only once
        verify(ipWhoIsGeoService, times(1)).lookup(ip);
        
        // Cache should have 2 hits (after first miss)
        assertEquals(2, geoIpCache.stats().hitCount());
        assertEquals(1, geoIpCache.stats().missCount());
    }

    // ── API fallback chain tests ──────────────────────────────────────────────

    @Test
    void resolve_ipWhoIsNoCurrency_shouldFallbackToIpApiCom() {
        // Given
        String ip = "203.0.113.200";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        
        // ipwho.is returns country only (no currency - free plan)
        GeoResolution ipWhoIsResult = GeoResolution.countryOnly("SN");
        when(ipWhoIsGeoService.lookup(ip)).thenReturn(Optional.of(ipWhoIsResult));
        
        // ip-api.com returns country + currency
        GeoResolution ipApiResult = new GeoResolution("SN", "XOF");
        when(ipApiComGeoService.lookup(ip)).thenReturn(Optional.of(ipApiResult));

        // When
        Optional<GeoResolution> result = service.resolve(request);

        // Then - Should use ip-api.com result (has currency)
        assertTrue(result.isPresent());
        assertEquals("SN", result.get().countryCode());
        assertEquals("XOF", result.get().currencyCode());
        
        // Both APIs should be called
        verify(ipWhoIsGeoService, times(1)).lookup(ip);
        verify(ipApiComGeoService, times(1)).lookup(ip);
        
        // Result should be cached
        GeoResolution cached = geoIpCache.getIfPresent(ip);
        assertNotNull(cached);
        assertEquals("XOF", cached.currencyCode());
    }

    @Test
    void resolve_bothApisFail_shouldReturnEmpty() {
        // Given
        String ip = "203.0.113.50";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        
        when(ipWhoIsGeoService.lookup(ip)).thenReturn(Optional.empty());
        when(ipApiComGeoService.lookup(ip)).thenReturn(Optional.empty());

        // When
        Optional<GeoResolution> result = service.resolve(request);

        // Then
        assertFalse(result.isPresent());
        
        // Both APIs should be called
        verify(ipWhoIsGeoService, times(1)).lookup(ip);
        verify(ipApiComGeoService, times(1)).lookup(ip);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void resolve_nullRequest_shouldReturnEmpty() {
        // When
        Optional<GeoResolution> result = service.resolve(null);

        // Then
        assertFalse(result.isPresent());
        verify(ipWhoIsGeoService, never()).lookup(anyString());
    }

    @Test
    void resolve_blankIp_shouldReturnEmpty() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("   ");

        // When
        Optional<GeoResolution> result = service.resolve(request);

        // Then
        assertFalse(result.isPresent());
        verify(ipWhoIsGeoService, never()).lookup(anyString());
    }

    @Test
    void resolve_testOverride_shouldBypassCacheAndApis() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        
        when(environment.getProperty("geoip.country-test-override")).thenReturn("SN:XOF");

        // When
        Optional<GeoResolution> result = service.resolve(request);

        // Then
        assertTrue(result.isPresent());
        assertEquals("SN", result.get().countryCode());
        assertEquals("XOF", result.get().currencyCode());
        
        // Should bypass cache and APIs
        verify(ipWhoIsGeoService, never()).lookup(anyString());
        verify(ipApiComGeoService, never()).lookup(anyString());
        assertEquals(0, geoIpCache.stats().requestCount());
    }

    @Test
    void resolve_differentIps_shouldCacheIndependently() {
        // Given
        String ip1 = "203.0.113.1";
        String ip2 = "203.0.113.2";
        
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr(ip1);
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr(ip2);
        
        when(ipWhoIsGeoService.lookup(ip1)).thenReturn(Optional.of(new GeoResolution("CA", "CAD")));
        when(ipWhoIsGeoService.lookup(ip2)).thenReturn(Optional.of(new GeoResolution("US", "USD")));

        // When
        Optional<GeoResolution> result1 = service.resolve(request1);
        Optional<GeoResolution> result2 = service.resolve(request2);
        
        // Call again to test cache
        Optional<GeoResolution> result1Again = service.resolve(request1);
        Optional<GeoResolution> result2Again = service.resolve(request2);

        // Then
        assertEquals("CAD", result1.get().currencyCode());
        assertEquals("USD", result2.get().currencyCode());
        assertEquals("CAD", result1Again.get().currencyCode());
        assertEquals("USD", result2Again.get().currencyCode());
        
        // Each IP should be looked up once
        verify(ipWhoIsGeoService, times(1)).lookup(ip1);
        verify(ipWhoIsGeoService, times(1)).lookup(ip2);
        
        // Cache should have 2 entries and 2 hits
        assertEquals(2, geoIpCache.estimatedSize());
        assertEquals(2, geoIpCache.stats().hitCount());
    }
}
