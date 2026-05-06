package com.lmp.catalog.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.catalog.domain.ServiceCategory;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.catalog.repository.ServiceCategoryRepository;
import com.lmp.catalog.repository.ServiceOfferRepository;
import com.lmp.catalog.repository.ServiceRepository;

/**
 * Service métier pour le catalogue de services.
 * Fournit la logique de sélection d'offre valide (promo > défaut).
 */
@Service
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCatalogService.class);

        private final ServiceRepository serviceRepository;

        private final ServiceOfferRepository offerRepository;

        private final ServiceCategoryRepository categoryRepository;


    public ServiceCatalogService(ServiceRepository serviceRepository,
                           ServiceOfferRepository offerRepository,
                           ServiceCategoryRepository categoryRepository) {
        this.serviceRepository = serviceRepository;
        this.offerRepository = offerRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Retourne tous les services actifs avec leur offre courante.
     * Cache Caffeine — invalidation manuelle via {@link #evictCatalogCaches()}
     * quand l'admin modifie un service (cf {@link com.lmp.catalog.web.api.AdminServiceRestController}).
     */
    @Cacheable("catalog-active-services")
    public List<com.lmp.catalog.domain.Service> getActiveServices() {
        return serviceRepository.findByActiveTrue();
    }

    /**
     * Retourne les services mis en avant (featured) pour la page d'accueil.
     */
    @Cacheable("catalog-featured-services")
    public List<com.lmp.catalog.domain.Service> getFeaturedServices() {
        return serviceRepository.findByFeaturedTrueAndActiveTrue();
    }

    /**
     * Retourne toutes les catégories avec leurs services.
     */
    @Cacheable("catalog-categories")
    public List<ServiceCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Invalide les caches du catalogue. À appeler depuis les endpoints admin
     * qui modifient les services / offres / catégories.
     */
    @CacheEvict(value = {"catalog-active-services", "catalog-featured-services", "catalog-categories"}, allEntries = true)
    public void evictCatalogCaches() {
        logger.info("Catalog caches evicted");
    }

    /**
     * Retourne un service par son ID.
     */
    public Optional<com.lmp.catalog.domain.Service> getServiceById(java.util.UUID id) {
        return serviceRepository.findById(id);
    }

    /**
     * Retourne un service par son slug.
     */
    public Optional<com.lmp.catalog.domain.Service> getServiceBySlug(String slug) {
        return serviceRepository.findBySlug(slug);
    }

    /**
     * Retourne une offre active par son ID, en vérifiant sa validité.
     * C'est la méthode clé pour sécuriser le checkout.
     *
     * @param offerId L'ID de l'offre
     * @return L'offre si elle est active et valide, Optional.empty() sinon
     */
    public Optional<ServiceOffer> getValidOffer(java.util.UUID offerId) {
        Optional<ServiceOffer> offerOpt = offerRepository.findByIdAndActiveTrue(offerId);
        if (offerOpt.isEmpty()) {
            logger.warn("Offre {} non trouvée ou inactive", offerId);
            return Optional.empty();
        }

        ServiceOffer offer = offerOpt.get();
        if (!offer.isCurrentlyValid()) {
            logger.warn("Offre {} existe mais n'est plus valide temporellement", offerId);
            return Optional.empty();
        }

        return Optional.of(offer);
    }

    /**
     * Retourne l'offre courante d'un service (promo valide > défaut).
     */
    public Optional<ServiceOffer> getCurrentOfferForService(java.util.UUID serviceId) {
        Optional<com.lmp.catalog.domain.Service> serviceOpt = serviceRepository.findById(serviceId);
        if (serviceOpt.isEmpty()) {
            return Optional.empty();
        }

        ServiceOffer currentOffer = serviceOpt.get().getCurrentOffer();
        return Optional.ofNullable(currentOffer);
    }

    // ========== Admin CRUD Operations ==========

    @Transactional
    public ServiceCategory saveCategory(ServiceCategory category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(java.util.UUID categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Transactional
    public com.lmp.catalog.domain.Service saveService(com.lmp.catalog.domain.Service service) {
        service.setUpdatedAt(java.time.LocalDateTime.now());
        if (service.getCreatedAt() == null) {
            service.setCreatedAt(java.time.LocalDateTime.now());
        }
        return serviceRepository.save(service);
    }

    @Transactional
    public void deleteService(java.util.UUID serviceId) {
        serviceRepository.deleteById(serviceId);
    }

    @Transactional
    public ServiceOffer saveOffer(ServiceOffer offer) {
        return offerRepository.save(offer);
    }

    @Transactional
    public void deleteOffer(java.util.UUID offerId) {
        offerRepository.deleteById(offerId);
    }
}
