package com.lmp.catalog.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.catalog.dto.ServiceResponse;
import com.lmp.shared.pricing.RegionalPricingService;
import com.lmp.shared.web.PrecompressedResponse;

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
 * API REST du catalogue de services. Endpoints publics — pas d'authentification.
 *
 * <p><b>Optim 2026-05-17:</b> response cached en {@link PrecompressedResponse}
 * (raw + gzip pre-compressed at build time). Sub-50µs serve sur cache hit.
 * Skip Jackson serialize ET gzip compress per-request.</p>
 */
@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services", description = "Catalogue de services et offres")
public class ServiceRestController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ServiceCatalogService catalogService;
    private final RegionalPricingService regionalPricingService;
    private final ObjectMapper objectMapper;

    private final Map<String, PrecompressedResponse> featuredCache = new ConcurrentHashMap<>();
    private final Map<String, PrecompressedResponse> searchCache = new ConcurrentHashMap<>();
    private final Map<String, PrecompressedResponse> slugCache = new ConcurrentHashMap<>();

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
        PrecompressedResponse r = cachedOrBuild(featuredCache, country, () -> {
            List<ServiceResponse> services = catalogService.getFeaturedServices().stream()
                    .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsBytes(ApiResponse.ok(services));
        });
        return serve(httpRequest, r);
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
        PrecompressedResponse r = cachedOrBuild(searchCache, key, () -> {
            List<ServiceResponse> results = catalogService.searchActive(query.trim(), safeLimit).stream()
                    .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsBytes(ApiResponse.ok(results));
        });
        return serve(httpRequest, r);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un service", description = "Retourne un service par son slug")
    public ResponseEntity<byte[]> getServiceBySlug(
            @PathVariable String slug,
            HttpServletRequest httpRequest) throws IOException {
        var ctx = regionalPricingService.resolve(httpRequest);
        String key = slug + "|" + ctx.countryCode();
        PrecompressedResponse c = slugCache.get(key);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return serveWithVary(httpRequest, c);
        }
        var serviceOpt = catalogService.getServiceBySlug(slug);
        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = objectMapper.writeValueAsBytes(
                ApiResponse.ok(ServiceResponse.from(serviceOpt.get(), ctx, regionalPricingService)));
        PrecompressedResponse r = PrecompressedResponse.build(body, now.plus(CACHE_TTL));
        slugCache.put(key, r);
        return serveWithVary(httpRequest, r);
    }

    private ResponseEntity<byte[]> serve(HttpServletRequest req, PrecompressedResponse r) {
        return serveCommon(req, r, "Accept-Encoding");
    }

    private ResponseEntity<byte[]> serveWithVary(HttpServletRequest req, PrecompressedResponse r) {
        return serveCommon(req, r, VARY_GEO + ", Accept-Encoding");
    }

    private ResponseEntity<byte[]> serveCommon(HttpServletRequest req, PrecompressedResponse r, String varyHeader) {
        // ETag revalidation : si client envoie If-None-Match qui match → 304 sans body
        String ifNoneMatch = req.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.contains(r.etag())) {
            return ResponseEntity.status(304)
                    .cacheControl(REGIONAL_CACHE)
                    .header("ETag", r.etag())
                    .header("Vary", varyHeader)
                    .build();
        }
        boolean gz = acceptsGzip(req);
        ResponseEntity.BodyBuilder b = ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .header("ETag", r.etag())
                .header("Vary", varyHeader)
                .contentType(MediaType.APPLICATION_JSON);
        if (gz) {
            b.header("Content-Encoding", "gzip");
            return b.body(r.gzip());
        }
        return b.body(r.raw());
    }

    private static boolean acceptsGzip(HttpServletRequest req) {
        String ae = req.getHeader("Accept-Encoding");
        return ae != null && ae.contains("gzip");
    }

    private PrecompressedResponse cachedOrBuild(Map<String, PrecompressedResponse> cache, String key,
                                                  ByteSupplier supplier) throws IOException {
        PrecompressedResponse c = cache.get(key);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return c;
        }
        byte[] bytes = supplier.get();
        PrecompressedResponse r = PrecompressedResponse.build(bytes, now.plus(CACHE_TTL));
        cache.put(key, r);
        return r;
    }

    @FunctionalInterface
    private interface ByteSupplier {
        byte[] get() throws IOException;
    }
}
