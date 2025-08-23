package com.lmp.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer les données des services offerts par LMP
 * 
 * Ce service centralise les informations sur les services pour éviter
 * la duplication de code entre les contrôleurs.
 */
@Service
public class ServicesDataService {
    
    /**
     * Représente un service offert par LMP
     */
    public static class ServiceInfo {
        private final String title;
        private final String description;
        private final String icon;
        private final List<String> benefits;
        private final String price;
        private final double priceValue;
        private final String duration;
        private final String category;
        
        public ServiceInfo(String title, String description, String icon, List<String> benefits, 
                          String price, double priceValue, String duration, String category) {
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.benefits = benefits;
            this.price = price;
            this.priceValue = priceValue;
            this.duration = duration;
            this.category = category;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public List<String> getBenefits() { return benefits; }
        public String getPrice() { return price; }
        public double getPriceValue() { return priceValue; }
        public String getDuration() { return duration; }
        public String getCategory() { return category; }
    }
    
    /**
     * Retourne la liste complète des services
     */
    public List<ServiceInfo> getAllServices() {
        return List.of(
            new ServiceInfo(
                "Sécurisations et Accès Google My Business",
                "Protégez et optimisez votre profil Google My Business. Accès sécurisé, gestion des droits et protection contre les modifications malveillantes.",
                "📍",
                List.of(
                    "Rapports mensuels de votre technicien",
                    "Assistance (12 mois)",
                    "Sécurisation du profil"
                ),
                "353,89 $",
                353.89,
                "60 minutes",
                "Référencement Local"
            ),
            new ServiceInfo(
                "Référencement Optimale avec Sécurisations Garantie VIP+",
                "Service premium avec garantie de résultats. Référencement avancé, maintenance du profil et support prioritaire.",
                "⭐",
                List.of(
                    "5 mots clés garantis",
                    "Maintenance du profile",
                    "QR code compte publicitaire Facebook"
                ),
                "750,79 $",
                750.79,
                "90 minutes",
                "Référencement Premium"
            ),
            new ServiceInfo(
                "Gestion des Avis",
                "Améliorez votre réputation en ligne avec notre service de gestion des avis. Multipliez les avis positifs et gérez efficacement les commentaires.",
                "💬",
                List.of(
                    "Soyez le mieux noté de votre secteur",
                    "Gérez les avis indésirables",
                    "Recevez plus d'avis positifs"
                ),
                "747,43 $",
                747.43,
                "75 minutes",
                "Réputation en Ligne"
            ),
            new ServiceInfo(
                "Présence Locales",
                "Boostez votre visibilité locale avec une présence optimisée dans tous les annuaires locaux et sur Google Maps.",
                "🏪",
                List.of(
                    "Montez dans les résultats de la carte Google",
                    "Boostez votre visibilité dans votre ville",
                    "Visible dans tous les annuaires locaux"
                ),
                "2200,00 $",
                2200.00,
                "120 minutes",
                "Marketing Local"
            ),
            new ServiceInfo(
                "Création Site Web",
                "Création de site web professionnel avec protocole SSL, référencement optimisé et promotion incluse.",
                "💻",
                List.of(
                    "Création + promotion avec protocole SSL",
                    "Référencement parmi les meilleurs résultats",
                    "Boost des pages (publicité)"
                ),
                "100,99 $",
                100.99,
                "180 minutes",
                "Développement Web"
            ),
            new ServiceInfo(
                "SEO et Référencement Naturel",
                "Service SEO complet pour améliorer votre positionnement sur les moteurs de recherche et Google Maps.",
                "🔍",
                List.of(
                    "Améliorez votre positionnement sur les moteurs",
                    "Mise à jour régulière des données GMB",
                    "Résultats garantis pour Google Maps"
                ),
                "5500,00 $",
                5500.00,
                "240 minutes",
                "SEO Avancé"
            ),
            new ServiceInfo(
                "Optimisations Google+",
                "Optimisation premium de votre présence Google avec mise à jour régulière et positionnement garanti.",
                "🚀",
                List.of(
                    "Améliorez votre positionnement sur les moteurs",
                    "Mise à jour régulière des données GMB",
                    "Résultats garantis / idéal pour entreprises"
                ),
                "11121,00 $",
                11121.00,
                "300 minutes",
                "Google Premium"
            ),
            new ServiceInfo(
                "Assistance Technique",
                "Support technique dédié pour vous aider à rester devant vos concurrents avec une maintenance de votre profil.",
                "🛠️",
                List.of(
                    "Mettez vous devant vos concurrents",
                    "Soyez en tête des recherches",
                    "Maintenance de votre profil"
                ),
                "402,00 $",
                402.00,
                "45 minutes",
                "Support"
            )
        );
    }
    
    /**
     * Retourne les services principaux (pour la page d'accueil) - limité à 6 services
     */
    public List<ServiceInfo> getFeaturedServices() {
        List<ServiceInfo> allServices = getAllServices();
        // Retourne les 6 premiers services pour la page d'accueil
        return allServices.subList(0, Math.min(6, allServices.size()));
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
