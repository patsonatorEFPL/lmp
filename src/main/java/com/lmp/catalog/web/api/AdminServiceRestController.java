package com.lmp.catalog.web.api;

import com.lmp.catalog.domain.*;
import com.lmp.catalog.dto.ServiceResponse;
import com.lmp.catalog.repository.*;
import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API REST d'administration du catalogue de services.
 * Endpoints ADMIN-only pour CRUD services, catégories, offres et avantages.
 */
@RestController
@RequestMapping("/api/v1/admin/services")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Services", description = "CRUD catalogue de services (ADMIN only)")
public class AdminServiceRestController {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceRestController.class);

    private final ServiceCatalogService catalogService;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceOfferRepository offerRepository;
    private final ServiceBenefitRepository benefitRepository;
    private final OfferBenefitRepository offerBenefitRepository;

    public AdminServiceRestController(ServiceCatalogService catalogService,
                                       ServiceCategoryRepository categoryRepository,
                                       ServiceRepository serviceRepository,
                                       ServiceOfferRepository offerRepository,
                                       ServiceBenefitRepository benefitRepository,
                                       OfferBenefitRepository offerBenefitRepository) {
        this.catalogService = catalogService;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.offerRepository = offerRepository;
        this.benefitRepository = benefitRepository;
        this.offerBenefitRepository = offerBenefitRepository;
    }

    // ========== Dashboard stats ==========

    @GetMapping("/stats")
    @Operation(summary = "Statistiques catalogue", description = "KPIs catégories, services, offres")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCatalogStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCategories", categoryRepository.count());
        stats.put("totalServices", serviceRepository.count());
        stats.put("totalOffers", offerRepository.count());
        stats.put("activeServices", serviceRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive())).count());
        stats.put("featuredServices", serviceRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getFeatured())).count());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ========== Categories ==========

    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories() {
        List<ServiceCategory> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        List<Map<String, Object>> result = categories.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("slug", c.getSlug());
            map.put("description", c.getDescription());
            map.put("icon", c.getIcon());
            map.put("displayOrder", c.getDisplayOrder());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/categories")
    @Operation(summary = "Créer une catégorie")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCategory(@RequestBody Map<String, Object> data) {
        try {
            ServiceCategory category = new ServiceCategory();
            category.setName((String) data.get("name"));
            category.setSlug(slugify((String) data.get("name")));
            category.setDescription((String) data.getOrDefault("description", ""));
            category.setIcon((String) data.getOrDefault("icon", "📁"));
            category.setDisplayOrder(data.containsKey("displayOrder") ? ((Number) data.get("displayOrder")).intValue() : 0);
            category = categoryRepository.save(category);
            return ResponseEntity.ok(ApiResponse.ok("Catégorie créée",
                    Map.of("id", category.getId(), "name", category.getName())));
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Modifier une catégorie")
    public ResponseEntity<ApiResponse<Void>> updateCategory(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            ServiceCategory category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            if (data.containsKey("name")) category.setName((String) data.get("name"));
            if (data.containsKey("description")) category.setDescription((String) data.get("description"));
            if (data.containsKey("icon")) category.setIcon((String) data.get("icon"));
            if (data.containsKey("displayOrder")) category.setDisplayOrder(((Number) data.get("displayOrder")).intValue());
            categoryRepository.save(category);
            return ResponseEntity.ok(ApiResponse.ok("Catégorie mise à jour", null));
        } catch (Exception e) {
            logger.error("Error updating category {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Supprimer une catégorie")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        try {
            categoryRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok("Catégorie supprimée", null));
        } catch (Exception e) {
            logger.error("Error deleting category {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Impossible de supprimer (services liés ?)"));
        }
    }

    // ========== Services ==========

    @GetMapping
    @Operation(summary = "Lister tous les services (admin)", description = "Inclut les services inactifs")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices() {
        List<ServiceResponse> services = serviceRepository.findAll().stream()
                .sorted(Comparator.comparingInt(s -> s.getDisplayOrder() != null ? s.getDisplayOrder() : 0))
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(services));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un service (admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getService(@PathVariable UUID id) {
        try {
            Service service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));
            List<ServiceOffer> offers = offerRepository.findByServiceId(id);
            List<ServiceBenefit> benefitsList = service.getBenefits() != null ?
                    service.getBenefits().stream().toList() : List.of();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("service", ServiceResponse.from(service));
            result.put("categoryId", service.getCategory() != null ? service.getCategory().getId() : null);
            result.put("offers", offers.stream().map(o -> {
                List<OfferBenefit> ob = offerBenefitRepository.findByOfferIdOrderByDisplayOrderAsc(o.getId());
                Map<String, Object> offerMap = new LinkedHashMap<>();
                offerMap.put("id", o.getId());
                offerMap.put("name", o.getName());
                offerMap.put("price", o.getPrice());
                offerMap.put("originalPrice", o.getOriginalPrice());
                offerMap.put("durationType", o.getDurationType() != null ? o.getDurationType().name() : null);
                offerMap.put("isDefault", o.getIsDefault());
                offerMap.put("active", o.getActive());
                offerMap.put("benefits", ob.stream().map(b -> Map.of("id", b.getId(), "benefit", b.getBenefit())).toList());
                return offerMap;
            }).toList());
            result.put("benefits", benefitsList.stream().map(b -> Map.of("id", b.getId(), "benefit", b.getBenefit())).toList());

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            logger.error("Error getting service {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Créer un service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createService(@RequestBody Map<String, Object> data) {
        try {
            ServiceCategory category = categoryRepository.findById(UUID.fromString(String.valueOf(data.get("categoryId"))))
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            Service service = new Service();
            service.setCategory(category);
            service.setTitle((String) data.get("title"));
            service.setSlug(slugify((String) data.get("title")));
            service.setDescription((String) data.getOrDefault("description", ""));
            service.setIcon((String) data.getOrDefault("icon", "📦"));
            service.setDisplayOrder(data.containsKey("displayOrder") ? ((Number) data.get("displayOrder")).intValue() : 0);
            service.setFeatured(Boolean.TRUE.equals(data.get("featured")));
            service.setActive(data.containsKey("active") ? Boolean.TRUE.equals(data.get("active")) : true);
            service.setCreatedAt(LocalDateTime.now());
            service.setUpdatedAt(LocalDateTime.now());

            // Save service
            service = serviceRepository.save(service);

            // Save benefits if provided
            @SuppressWarnings("unchecked")
            List<String> benefits = (List<String>) data.get("benefits");
            if (benefits != null) {
                for (String text : benefits) {
                    if (text != null && !text.trim().isEmpty()) {
                        ServiceBenefit b = new ServiceBenefit();
                        b.setService(service);
                        b.setBenefit(text.trim());
                        benefitRepository.save(b);
                    }
                }
            }

            return ResponseEntity.ok(ApiResponse.ok("Service créé",
                    Map.of("id", service.getId(), "title", service.getTitle())));
        } catch (Exception e) {
            logger.error("Error creating service: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un service")
    public ResponseEntity<ApiResponse<Void>> updateService(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            Service service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));
            if (data.containsKey("title")) service.setTitle((String) data.get("title"));
            if (data.containsKey("description")) service.setDescription((String) data.get("description"));
            if (data.containsKey("icon")) service.setIcon((String) data.get("icon"));
            if (data.containsKey("displayOrder")) service.setDisplayOrder(((Number) data.get("displayOrder")).intValue());
            if (data.containsKey("featured")) service.setFeatured(Boolean.TRUE.equals(data.get("featured")));
            if (data.containsKey("active")) service.setActive(Boolean.TRUE.equals(data.get("active")));
            if (data.containsKey("categoryId")) {
                ServiceCategory category = categoryRepository.findById(UUID.fromString(String.valueOf(data.get("categoryId"))))
                        .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
                service.setCategory(category);
            }
            service.setUpdatedAt(LocalDateTime.now());
            serviceRepository.save(service);
            return ResponseEntity.ok(ApiResponse.ok("Service mis à jour", null));
        } catch (Exception e) {
            logger.error("Error updating service {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un service")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable UUID id) {
        try {
            serviceRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok("Service supprimé", null));
        } catch (Exception e) {
            logger.error("Error deleting service {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Impossible de supprimer (commandes liées ?)"));
        }
    }

    // ========== Offers ==========

    @PostMapping("/{serviceId}/offers")
    @Operation(summary = "Créer une offre pour un service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOffer(@PathVariable UUID serviceId, @RequestBody Map<String, Object> data) {
        try {
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));
            ServiceOffer offer = new ServiceOffer();
            offer.setService(service);
            offer.setName((String) data.get("name"));
            offer.setPrice(new BigDecimal(String.valueOf(data.get("price"))));
            if (data.containsKey("originalPrice") && data.get("originalPrice") != null
                    && !String.valueOf(data.get("originalPrice")).trim().isEmpty()) {
                offer.setOriginalPrice(new BigDecimal(String.valueOf(data.get("originalPrice"))));
            }
            offer.setDurationType(DurationType.valueOf((String) data.getOrDefault("durationType", "ONE_TIME")));
            offer.setIsDefault(Boolean.TRUE.equals(data.get("isDefault")));
            offer.setActive(data.containsKey("active") ? Boolean.TRUE.equals(data.get("active")) : true);
            offer = offerRepository.save(offer);
            return ResponseEntity.ok(ApiResponse.ok("Offre créée", Map.of("id", offer.getId())));
        } catch (Exception e) {
            logger.error("Error creating offer: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/offers/{id}")
    @Operation(summary = "Modifier une offre")
    public ResponseEntity<ApiResponse<Void>> updateOffer(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            ServiceOffer offer = offerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Offre non trouvée"));
            if (data.containsKey("name")) offer.setName((String) data.get("name"));
            if (data.containsKey("price")) offer.setPrice(new BigDecimal(String.valueOf(data.get("price"))));
            if (data.containsKey("originalPrice") && data.get("originalPrice") != null
                    && !String.valueOf(data.get("originalPrice")).trim().isEmpty()) {
                offer.setOriginalPrice(new BigDecimal(String.valueOf(data.get("originalPrice"))));
            }
            if (data.containsKey("durationType")) offer.setDurationType(DurationType.valueOf((String) data.get("durationType")));
            if (data.containsKey("isDefault")) offer.setIsDefault(Boolean.TRUE.equals(data.get("isDefault")));
            if (data.containsKey("active")) offer.setActive(Boolean.TRUE.equals(data.get("active")));
            offerRepository.save(offer);
            return ResponseEntity.ok(ApiResponse.ok("Offre mise à jour", null));
        } catch (Exception e) {
            logger.error("Error updating offer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/offers/{id}")
    @Operation(summary = "Supprimer une offre")
    public ResponseEntity<ApiResponse<Void>> deleteOffer(@PathVariable UUID id) {
        try {
            offerRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok("Offre supprimée", null));
        } catch (Exception e) {
            logger.error("Error deleting offer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Benefits ==========

    @PostMapping("/{serviceId}/benefits")
    @Operation(summary = "Ajouter un avantage à un service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBenefit(@PathVariable UUID serviceId, @RequestBody Map<String, Object> data) {
        try {
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));
            ServiceBenefit benefit = new ServiceBenefit();
            benefit.setService(service);
            benefit.setBenefit((String) data.get("benefit"));
            benefit = benefitRepository.save(benefit);
            return ResponseEntity.ok(ApiResponse.ok("Avantage ajouté", Map.of("id", benefit.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/benefits/{id}")
    @Operation(summary = "Supprimer un avantage")
    public ResponseEntity<ApiResponse<Void>> deleteBenefit(@PathVariable UUID id) {
        try {
            benefitRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok("Avantage supprimé", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{serviceId}/benefits/sync")
    @Transactional
    @Operation(summary = "Synchroniser les avantages d'un service")
    public ResponseEntity<ApiResponse<Void>> syncBenefits(@PathVariable UUID serviceId, @RequestBody Map<String, Object> data) {
        try {
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));

            // Clear the Hibernate-managed collection to prevent cascade re-persistence
            if (service.getBenefits() != null) {
                service.getBenefits().clear();
            }

            // Bulk delete existing benefits via JPQL (bypasses Hibernate collection management)
            benefitRepository.deleteAllByServiceId(serviceId);
            benefitRepository.flush(); // Ensure deletes are committed before inserts

            // Add new benefits
            @SuppressWarnings("unchecked")
            List<String> benefits = (List<String>) data.get("benefits");
            if (benefits != null) {
                for (String text : benefits) {
                    if (text != null && !text.trim().isEmpty()) {
                        ServiceBenefit b = new ServiceBenefit();
                        b.setService(service);
                        b.setBenefit(text.trim());
                        benefitRepository.save(b);
                    }
                }
            }
            return ResponseEntity.ok(ApiResponse.ok("Avantages mis à jour", null));
        } catch (Exception e) {
            logger.error("Error syncing benefits for service {}: {}", serviceId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Helpers ==========

    private String slugify(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[ôö]", "o")
                .replaceAll("[îï]", "i")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
