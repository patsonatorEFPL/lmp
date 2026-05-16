package com.lmp.catalog.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.catalog.dto.ServiceResponse;
import com.lmp.shared.pricing.RegionalPricingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * API REST du catalogue de services.
 * Endpoints publics — pas d'authentification requise.
 *
 * <p><b>Optim 2026-05-16:</b> response pre-sérialisée en byte[] cached par
 * (path, country code) — bench 5k VU mix réaliste a montré endpoints
 * featured/search comme hot path. TTL 5 min aligné Cache-Control.</p>
 */
@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services", description = "Catalogue de services et offres")
public class ServiceRestController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ServiceCatalogService catalogService;
    private final RegionalPricingService regionalPricingService;
    private final ObjectMapper objectMapper;

    /** Cache par-region des byte[] response pour /featured. Key = countryCode. */
    private final Map<String, CachedResponse> featuredCache = new ConcurrentHashMap<>();
    /** Cache par (query+limit+countryCode) des byte[] response pour /search. */
    private final Map<String, CachedResponse> searchCache = new ConcurrentHashMap<>();
    /** Cache par (slug+countryCode) des byte[] response pour /{slug}. */
    private final Map<String, CachedResponse> slugCache = new ConcurrentHashMap<>();

    public ServiceRestController(ServiceCatalogService catalogService,
            RegionalPricingService regionalPricingService,
            ObjectMapper objectMapper) {
        this.catalogService = catalogService;
        this.regionalPricingService = regionalPricingService;
        this.objectMapper = objectMapper;
    }

    private static final CacheControl REGIONAL_CACHE = CacheControl
            .maxAge(Duration.ofMinutes(5))
            .cachePublic();
    private static final String VARY_GEO = "CF-IPCountry";

    @Transactional(readOnly = true)
    @GetMapping
    @Operation(summary = "Lister les services", description = "Retourne tous les services actifs avec leurs offres")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices(HttpServletRequest httpRequest) {
        var ctx = regionalPricingService.resolve(httpRequest);
        List<ServiceResponse> services = catalogService.getActiveServices().stream()
                .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .body(ApiResponse.ok(services));
    }

    @GetMapping("/featured")
    @Operation(summary = "Services en vedette", description = "Retourne les services mis en avant")
    public ResponseEntity<byte[]> getFeaturedServices(HttpServletRequest httpRequest) throws IOException {
        var ctx = regionalPricingService.resolve(httpRequest);
        String country = ctx.countryCode();
        byte[] body = cachedOrBuild(featuredCache, country, () -> {
            List<ServiceResponse> services = catalogService.getFeaturedServices().stream()
                    .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsBytes(ApiResponse.ok(services));
        });
        return ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche full-text catalogue")
    public ResponseEntity<byte[]> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest httpRequest) throws IOException {
        if (query == null || query.isBlank()) {
            byte[] empty = objectMapper.writeValueAsBytes(ApiResponse.ok(List.of()));
            return ResponseEntity.ok()
                    .cacheControl(REGIONAL_CACHE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(empty);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        var ctx = regionalPricingService.resolve(httpRequest);
        String key = query.trim() + "|" + safeLimit + "|" + ctx.countryCode();
        byte[] body = cachedOrBuild(searchCache, key, () -> {
            List<ServiceResponse> results = catalogService.searchActive(query.trim(), safeLimit).stream()
                    .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsBytes(ApiResponse.ok(results));
        });
        return ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un service", description = "Retourne un service par son slug")
    public ResponseEntity<byte[]> getServiceBySlug(
            @PathVariable String slug,
            HttpServletRequest httpRequest) throws IOException {
        var ctx = regionalPricingService.resolve(httpRequest);
        String key = slug + "|" + ctx.countryCode();
        // Si présent en cache → 200 direct ; sinon build (404 possible)
        CachedResponse c = slugCache.get(key);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return ResponseEntity.ok()
                    .cacheControl(REGIONAL_CACHE)
                    .header("Vary", VARY_GEO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(c.bytes());
        }
        var serviceOpt = catalogService.getServiceBySlug(slug);
        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = objectMapper.writeValueAsBytes(
                ApiResponse.ok(ServiceResponse.from(serviceOpt.get(), ctx, regionalPricingService)));
        slugCache.put(key, new CachedResponse(body, now.plus(CACHE_TTL)));
        return ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .header("Vary", VARY_GEO)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * Cache lookup or rebuild. Race bénigne sur miss simultané = 2× compute,
     * 1× retained (last write wins). Pas de lock pour rester lock-free.
     */
    private byte[] cachedOrBuild(Map<String, CachedResponse> cache, String key,
                                  ByteSupplier supplier) throws IOException {
        CachedResponse c = cache.get(key);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return c.bytes();
        }
        byte[] bytes = supplier.get();
        cache.put(key, new CachedResponse(bytes, now.plus(CACHE_TTL)));
        return bytes;
    }

    @FunctionalInterface
    private interface ByteSupplier {
        byte[] get() throws IOException;
    }

    private record CachedResponse(byte[] bytes, Instant expiresAt) {}
}
