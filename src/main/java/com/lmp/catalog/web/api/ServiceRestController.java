package com.lmp.catalog.web.api;

import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.catalog.dto.ServiceResponse;
import com.lmp.shared.pricing.RegionalPricingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(summary = "Lister les services", description = "Retourne tous les services actifs avec leurs offres")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices(HttpServletRequest httpRequest) {
        var ctx = regionalPricingService.resolve(httpRequest);
        List<ServiceResponse> services = catalogService.getActiveServices().stream()
                .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(services));
    }

    @GetMapping("/featured")
    @Operation(summary = "Services en vedette", description = "Retourne les services mis en avant")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getFeaturedServices(HttpServletRequest httpRequest) {
        var ctx = regionalPricingService.resolve(httpRequest);
        List<ServiceResponse> services = catalogService.getFeaturedServices().stream()
                .map(s -> ServiceResponse.from(s, ctx, regionalPricingService))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(services));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un service", description = "Retourne un service par son slug")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceBySlug(
            @PathVariable String slug,
            HttpServletRequest httpRequest) {
        var ctx = regionalPricingService.resolve(httpRequest);
        return catalogService.getServiceBySlug(slug)
                .map(service -> ResponseEntity.ok(ApiResponse.ok(ServiceResponse.from(service, ctx, regionalPricingService))))
                .orElse(ResponseEntity.notFound().build());
    }
}
