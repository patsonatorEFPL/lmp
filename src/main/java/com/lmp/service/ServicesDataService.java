package com.lmp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lmp.domain.entity.ServiceBenefit;
import com.lmp.domain.entity.ServiceOffer;
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

    /**
     * Représente un service offert par LMP
     */
    public static class ServiceInfo {
        private final Long id;
        private final String title;
        private final String description;
        private final String icon;
        private final List<String> benefits;
        private final String price;
        private final double priceValue;
        private final String duration;
        private final String category;
        private final Long offerId;

        public ServiceInfo(Long id, String title, String description, String icon, List<String> benefits,
                String price, double priceValue, String duration, String category, Long offerId) {
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
        public Long getId() {
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

        public Long getOfferId() {
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
        Long offerId = currentOffer != null ? currentOffer.getId() : null;

        List<String> benefitStrings = service.getBenefits().stream()
                .map(ServiceBenefit::getBenefit)
                .collect(Collectors.toList());

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
