package com.lmp.catalog.web.api;

import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.catalog.dto.ServiceResponse;
import com.lmp.shared.pricing.RegionalPricingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API REST du catalogue de services.
 * Endpoints publics — pas d'authentification requise.
 */
@RestController
@RequestMapping("/api/v1/services")
@Transactional(readOnly = true)
@Tag(name = "Services", description = "Catalogue de services et offres")
public class ServiceRestController {

    private final ServiceCatalogService catalogService;
    private final RegionalPricingService regionalPricingService;

    public ServiceRestController(ServiceCatalogService catalogService,
            RegionalPricingService regionalPricingService) {
        this.catalogService = catalogService;
        this.regionalPricingService = regionalPricingService;
    }

    /**
     * Cache headers : prix dépendent du pays IP (geo). Cloudflare Cache Rule
     * doit inclure cf.geo.country dans la cache key, sinon pollution cross-region.
     * Vary: CF-IPCountry documente la dépendance pour caches respectant Vary.
     */
    private static final CacheControl REGIONAL_CACHE = CacheControl
            .maxAge(Duration.ofMinutes(5))
            .cachePublic();
    private static final String VARY_GEO = "CF-IPCountry";

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
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getFeaturedServices(HttpServletRequest httpRequest) {
        var ctx = regionalPricingService.resolve(httpRequest);
        List<ServiceResponse> services = catalogService.getFeaturedServices().stream()
                .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .body(ApiResponse.ok(services));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche full-text catalogue",
               description = "tsvector sur titre + description (services actifs). Phrases entre quotes, négation -mot, OR supportés.")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.ok()
                    .cacheControl(REGIONAL_CACHE)
                    .body(ApiResponse.ok(List.of()));
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        var ctx = regionalPricingService.resolve(httpRequest);
        List<ServiceResponse> results = catalogService.searchActive(query.trim(), safeLimit).stream()
                .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .cacheControl(REGIONAL_CACHE)
                .body(ApiResponse.ok(results));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un service", description = "Retourne un service par son slug")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceBySlug(
            @PathVariable String slug,
            HttpServletRequest httpRequest) {
        var ctx = regionalPricingService.resolve(httpRequest);
        return catalogService.getServiceBySlug(slug)
                .map(service -> ResponseEntity.ok()
                        .cacheControl(REGIONAL_CACHE)
                        .header("Vary", VARY_GEO)
                        .body(ApiResponse.ok(ServiceResponse.from(service, ctx, regionalPricingService))))
                .orElse(ResponseEntity.notFound().build());
    }
}
