package com.lmp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lmp.domain.entity.OfferBenefit;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.repository.OfferBenefitRepository;
import com.lmp.repository.ServiceRepository;

/**
 * Service pour gérer les données des services offerts par LMP
 * 
 * Ce service centralise les informations sur les services pour éviter
 * la duplication de code entre les contrôleurs.
 */
@Service
public class ServicesDataService {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private OfferBenefitRepository offerBenefitRepository;

    /**
     * Représente un service offert par LMP
     */
    public static class ServiceInfo {
        private final java.util.UUID id;
        private final String title;
        private final String description;
        private final String icon;
        private final List<String> benefits;
        private final String price;
        private final double priceValue;
        private final String duration;
        private final String category;
        private final java.util.UUID offerId;

        public ServiceInfo(java.util.UUID id, String title, String description, String icon, List<String> benefits,
                String price, double priceValue, String duration, String category, java.util.UUID offerId) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.benefits = benefits;
            this.price = price;
            this.priceValue = priceValue;
            this.duration = duration;
            this.category = category;
            this.offerId = offerId;
        }

        // Getters
        public java.util.UUID getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return icon;
        }

        public List<String> getBenefits() {
            return benefits;
        }

        public String getPrice() {
            return price;
        }

        public double getPriceValue() {
            return priceValue;
        }

        public String getDuration() {
            return duration;
        }

        public String getCategory() {
            return category;
        }

        public java.util.UUID getOfferId() {
            return offerId;
        }
    }

    /**
     * Retourne la liste complète des services
     */
    public List<ServiceInfo> getAllServices() {
        return serviceRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .map(this::mapToServiceInfo)
                .collect(Collectors.toList());
    }

    private ServiceInfo mapToServiceInfo(com.lmp.domain.entity.Service service) {
        ServiceOffer currentOffer = service.getCurrentOffer();
        double priceValue = currentOffer != null ? currentOffer.getPrice().doubleValue() : 0.0;
        String priceString = currentOffer != null ? String.format("%.2f €", priceValue).replace(".", ",") : "Sur devis";
        String durationStr = currentOffer != null ? currentOffer.getDurationType().name() : "N/A";
        java.util.UUID offerId = currentOffer != null ? currentOffer.getId() : null;

        // Read benefits from the current offer (OfferBenefit) instead of service-level (ServiceBenefit)
        List<String> benefitStrings;
        if (currentOffer != null) {
            List<OfferBenefit> offerBenefits = offerBenefitRepository
                    .findByOfferIdOrderByDisplayOrderAsc(currentOffer.getId());
            benefitStrings = offerBenefits.stream()
                    .map(OfferBenefit::getBenefit)
                    .collect(Collectors.toList());
        } else {
            benefitStrings = List.of();
        }

        String categoryName = service.getCategory() != null ? service.getCategory().getName() : "Non catégorisé";

        return new ServiceInfo(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getIcon(),
                benefitStrings,
                priceString,
                priceValue,
                durationStr,
                categoryName,
                offerId);
    }

    /**
     * Retourne les services mis en avant (featured) pour la page d'accueil.
     * Utilise le flag DB featured=true ; fallback aux 6 premiers si aucun featured.
     */
    public List<ServiceInfo> getFeaturedServices() {
        List<ServiceInfo> featured = serviceRepository.findByFeaturedTrueAndActiveTrue().stream()
                .map(this::mapToServiceInfo)
                .collect(Collectors.toList());

        if (featured.isEmpty()) {
            // Fallback: retourne les 6 premiers actifs
            List<ServiceInfo> all = getAllServices();
            return all.subList(0, Math.min(6, all.size()));
        }
        return featured;
    }

    /**
     * Retourne un service par son titre
     */
    public ServiceInfo getServiceByTitle(String title) {
        return getAllServices().stream()
                .filter(service -> service.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }
}
