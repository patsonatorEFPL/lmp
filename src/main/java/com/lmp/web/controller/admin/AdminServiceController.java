package com.lmp.web.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lmp.domain.entity.OfferBenefit;
import com.lmp.domain.entity.Service;
import com.lmp.domain.entity.ServiceBenefit;
import com.lmp.domain.entity.ServiceCategory;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.domain.entity.enums.DurationType;
import com.lmp.repository.OfferBenefitRepository;
import com.lmp.repository.ServiceBenefitRepository;
import com.lmp.repository.ServiceCategoryRepository;
import com.lmp.repository.ServiceOfferRepository;
import com.lmp.repository.ServiceRepository;
import com.lmp.service.catalog.ServiceCatalogService;

/**
 * Contrôleur d'administration CRUD pour les services, catégories et offres.
 */
@Controller
@RequestMapping("/admin/services")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + AdminServiceController.class.getName());

    @Autowired
    private ServiceCatalogService catalogService;

    @Autowired
    private ServiceCategoryRepository categoryRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceOfferRepository offerRepository;

    @Autowired
    private ServiceBenefitRepository benefitRepository;

    @Autowired
    private OfferBenefitRepository offerBenefitRepository;

    // ========== Page principale ==========

    @GetMapping
    public String servicesPage(Model model) {
        List<ServiceCategory> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        List<Service> services = serviceRepository.findAll();
        List<ServiceOffer> offers = offerRepository.findAll();

        // Eagerly load offer benefits for display
        for (ServiceOffer offer : offers) {
            List<OfferBenefit> benefits = offerBenefitRepository.findByOfferIdOrderByDisplayOrderAsc(offer.getId());
            offer.setBenefits(new java.util.LinkedHashSet<>(benefits));
        }

        model.addAttribute("categories", categories);
        model.addAttribute("services", services);
        model.addAttribute("offers", offers);
        model.addAttribute("durationTypes", DurationType.values());
        model.addAttribute("totalCategories", categories.size());
        model.addAttribute("totalServices", services.size());
        model.addAttribute("totalOffers", offers.size());

        return "admin/services";
    }

    // ========== API Catégories ==========

    @PostMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<?> createCategory(@RequestBody Map<String, Object> data) {
        try {
            ServiceCategory category = new ServiceCategory();
            category.setName((String) data.get("name"));
            category.setSlug(slugify((String) data.get("name")));
            category.setDescription((String) data.getOrDefault("description", ""));
            category.setIcon((String) data.getOrDefault("icon", "📁"));
            category.setDisplayOrder(data.containsKey("displayOrder") ? ((Number) data.get("displayOrder")).intValue() : 0);

            category = categoryRepository.save(category);
            auditLogger.info("Category created: {} (id={})", category.getName(), category.getId());

            return ResponseEntity.ok(Map.of("success", true, "message", "Catégorie créée", "id", category.getId()));
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCategory(@PathVariable java.util.UUID id, @RequestBody Map<String, Object> data) {
        try {
            ServiceCategory category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

            if (data.containsKey("name")) category.setName((String) data.get("name"));
            if (data.containsKey("description")) category.setDescription((String) data.get("description"));
            if (data.containsKey("icon")) category.setIcon((String) data.get("icon"));
            if (data.containsKey("displayOrder")) category.setDisplayOrder(((Number) data.get("displayOrder")).intValue());

            categoryRepository.save(category);
            auditLogger.info("Category updated: {} (id={})", category.getName(), id);

            return ResponseEntity.ok(Map.of("success", true, "message", "Catégorie mise à jour"));
        } catch (Exception e) {
            logger.error("Error updating category {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCategory(@PathVariable java.util.UUID id) {
        try {
            categoryRepository.deleteById(id);
            auditLogger.info("Category deleted: id={}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Catégorie supprimée"));
        } catch (Exception e) {
            logger.error("Error deleting category {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Impossible de supprimer (services liés ?)"));
        }
    }

    // ========== API Services ==========

    @GetMapping("/api/services/{id}")
    @ResponseBody
    public ResponseEntity<?> getService(@PathVariable java.util.UUID id) {
        try {
            Service service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));

            List<ServiceOffer> offers = offerRepository.findByServiceId(id);
            List<ServiceBenefit> benefits = service.getBenefits() != null ?
                    service.getBenefits().stream().toList() : List.of();

            return ResponseEntity.ok(Map.of(
                    "service", Map.of(
                            "id", service.getId(),
                            "title", service.getTitle(),
                            "slug", service.getSlug(),
                            "description", service.getDescription(),
                            "icon", service.getIcon() != null ? service.getIcon() : "",
                            "categoryId", service.getCategory().getId(),
                            "displayOrder", service.getDisplayOrder(),
                            "featured", service.getFeatured(),
                            "active", service.getActive()
                    ),
                    "offers", offers.stream().map(o -> {
                        List<OfferBenefit> ob = offerBenefitRepository.findByOfferIdOrderByDisplayOrderAsc(o.getId());
                        return Map.of(
                            "id", o.getId(),
                            "name", o.getName(),
                            "price", o.getPrice(),
                            "originalPrice", o.getOriginalPrice() != null ? o.getOriginalPrice() : "",
                            "durationType", o.getDurationType().name(),
                            "isDefault", o.getIsDefault(),
                            "active", o.getActive(),
                            "benefits", (Object) ob.stream().map(b -> Map.of(
                                    "id", b.getId(),
                                    "benefit", b.getBenefit()
                            )).toList()
                        );
                    }).toList(),
                    "benefits", benefits.stream().map(b -> Map.of(
                            "id", b.getId(),
                            "benefit", b.getBenefit()
                    )).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting service {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/services")
    @ResponseBody
    public ResponseEntity<?> createService(@RequestBody Map<String, Object> data) {
        try {
            ServiceCategory category = categoryRepository.findById(java.util.UUID.fromString(String.valueOf(data.get("categoryId"))))
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

            service = serviceRepository.save(service);
            auditLogger.info("Service created: {} (id={})", service.getTitle(), service.getId());

            return ResponseEntity.ok(Map.of("success", true, "message", "Service créé", "id", service.getId()));
        } catch (Exception e) {
            logger.error("Error creating service: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/api/services/{id}")
    @ResponseBody
    public ResponseEntity<?> updateService(@PathVariable java.util.UUID id, @RequestBody Map<String, Object> data) {
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
                ServiceCategory category = categoryRepository.findById(java.util.UUID.fromString(String.valueOf(data.get("categoryId"))))
                        .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
                service.setCategory(category);
            }
            service.setUpdatedAt(LocalDateTime.now());

            serviceRepository.save(service);
            auditLogger.info("Service updated: {} (id={})", service.getTitle(), id);

            return ResponseEntity.ok(Map.of("success", true, "message", "Service mis à jour"));
        } catch (Exception e) {
            logger.error("Error updating service {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/services/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteService(@PathVariable java.util.UUID id) {
        try {
            serviceRepository.deleteById(id);
            auditLogger.info("Service deleted: id={}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Service supprimé"));
        } catch (Exception e) {
            logger.error("Error deleting service {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Impossible de supprimer (commandes liées ?)"));
        }
    }

    // ========== API Offres ==========

    @PostMapping("/api/offers")
    @ResponseBody
    public ResponseEntity<?> createOffer(@RequestBody Map<String, Object> data) {
        try {
            Service service = serviceRepository.findById(java.util.UUID.fromString(String.valueOf(data.get("serviceId"))))
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
            auditLogger.info("Offer created: {} for service {} (id={})", offer.getName(), service.getTitle(), offer.getId());

            return ResponseEntity.ok(Map.of("success", true, "message", "Offre créée", "id", offer.getId()));
        } catch (Exception e) {
            logger.error("Error creating offer: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/api/offers/{id}")
    @ResponseBody
    public ResponseEntity<?> updateOffer(@PathVariable java.util.UUID id, @RequestBody Map<String, Object> data) {
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
            auditLogger.info("Offer updated: {} (id={})", offer.getName(), id);

            return ResponseEntity.ok(Map.of("success", true, "message", "Offre mise à jour"));
        } catch (Exception e) {
            logger.error("Error updating offer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/offers/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteOffer(@PathVariable java.util.UUID id) {
        try {
            offerRepository.deleteById(id);
            auditLogger.info("Offer deleted: id={}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Offre supprimée"));
        } catch (Exception e) {
            logger.error("Error deleting offer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== API Avantages ==========

    @PostMapping("/api/benefits")
    @ResponseBody
    public ResponseEntity<?> createBenefit(@RequestBody Map<String, Object> data) {
        try {
            Service service = serviceRepository.findById(java.util.UUID.fromString(String.valueOf(data.get("serviceId"))))
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));

            ServiceBenefit benefit = new ServiceBenefit();
            benefit.setService(service);
            benefit.setBenefit((String) data.get("benefit"));

            benefit = benefitRepository.save(benefit);
            return ResponseEntity.ok(Map.of("success", true, "message", "Avantage ajouté", "id", benefit.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/benefits/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteBenefit(@PathVariable java.util.UUID id) {
        try {
            benefitRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Avantage supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== API Avantages par Offre ==========

    @GetMapping("/api/offers/{offerId}/benefits")
    @ResponseBody
    public ResponseEntity<?> getOfferBenefits(@PathVariable java.util.UUID offerId) {
        try {
            List<OfferBenefit> benefits = offerBenefitRepository.findByOfferIdOrderByDisplayOrderAsc(offerId);
            return ResponseEntity.ok(benefits.stream().map(b -> Map.of(
                    "id", b.getId(),
                    "benefit", b.getBenefit(),
                    "displayOrder", b.getDisplayOrder() != null ? b.getDisplayOrder() : 0
            )).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/offers/{offerId}/benefits")
    @ResponseBody
    public ResponseEntity<?> addOfferBenefit(@PathVariable java.util.UUID offerId, @RequestBody Map<String, Object> data) {
        try {
            ServiceOffer offer = offerRepository.findById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offre non trouvée"));

            OfferBenefit benefit = new OfferBenefit();
            benefit.setOffer(offer);
            benefit.setBenefit((String) data.get("benefit"));
            benefit.setDisplayOrder(data.containsKey("displayOrder") ? ((Number) data.get("displayOrder")).intValue() : 0);

            benefit = offerBenefitRepository.save(benefit);
            return ResponseEntity.ok(Map.of("success", true, "message", "Avantage ajouté", "id", benefit.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/offer-benefits/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteOfferBenefit(@PathVariable java.util.UUID id) {
        try {
            offerBenefitRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Avantage supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/offers/{offerId}/benefits/sync")
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> syncOfferBenefits(@PathVariable java.util.UUID offerId, @RequestBody Map<String, Object> data) {
        try {
            ServiceOffer offer = offerRepository.findById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offre non trouvée"));

            // Delete existing benefits for this offer
            List<OfferBenefit> existing = offerBenefitRepository.findByOfferIdOrderByDisplayOrderAsc(offerId);
            offerBenefitRepository.deleteAll(existing);

            // Add new benefits
            @SuppressWarnings("unchecked")
            List<String> benefits = (List<String>) data.get("benefits");
            if (benefits != null) {
                for (int i = 0; i < benefits.size(); i++) {
                    String text = benefits.get(i);
                    if (text != null && !text.trim().isEmpty()) {
                        OfferBenefit ob = new OfferBenefit();
                        ob.setOffer(offer);
                        ob.setBenefit(text.trim());
                        ob.setDisplayOrder(i);
                        offerBenefitRepository.save(ob);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Avantages mis à jour"));
        } catch (Exception e) {
            logger.error("Error syncing offer benefits for offer {}: {}", offerId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/offers/{offerId}/benefits/copy-from/{sourceOfferId}")
    @ResponseBody
    public ResponseEntity<?> copyBenefitsFromOffer(@PathVariable java.util.UUID offerId, @PathVariable java.util.UUID sourceOfferId) {
        try {
            ServiceOffer targetOffer = offerRepository.findById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offre cible non trouvée"));

            List<OfferBenefit> sourceBenefits = offerBenefitRepository.findByOfferIdOrderByDisplayOrderAsc(sourceOfferId);

            for (OfferBenefit sb : sourceBenefits) {
                OfferBenefit newBenefit = new OfferBenefit();
                newBenefit.setOffer(targetOffer);
                newBenefit.setBenefit(sb.getBenefit());
                newBenefit.setDisplayOrder(sb.getDisplayOrder());
                offerBenefitRepository.save(newBenefit);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", sourceBenefits.size() + " avantage(s) copiés"));
        } catch (Exception e) {
            logger.error("Error copying benefits from offer {} to {}: {}", sourceOfferId, offerId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
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
