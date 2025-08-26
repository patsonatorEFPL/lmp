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
                "Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti. Synchronisation complète du profil avec votre courriel professionnel et sécurisation avancée contre toute modification non autorisée.",
                "📍",
                List.of(
                    "Remise des accès propriétaire principal",
                    "Synchronisation du profil Google avec votre courriel",
                    "Sécurisation du Google My Business"
                ),
                "353,89 $",
                353.89,
                "Paiement Unique",
                "Référencement Local"
            ),
            new ServiceInfo(
                "Référencement Optimale avec Sécurisations Garantie VIP+",
                "Service premium exclusif avec garantie de résultats exceptionnels. Bénéficiez de 8 mots clés stratégiques et 8 zones de services optimisées pour dominer votre marché local.",
                "⭐",
                List.of(
                    "8 mots clés garantis",
                    "8 zones de services",
                    "Remise et sécurisation des accès garanti",
                    "Remise des accès propriétaire principal",
                    "Validation du profil Google"
                ),
                "750,79 $",
                750.79,
                "Paiement Unique",
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
                "Paiement Unique",
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
                "Paiement Unique",
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
                "550,00 $",
                550.00,
                "Paiement Unique",
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
                "Paiement Unique",
                "SEO Avancé"
            ),
            new ServiceInfo(
                "Mise à jour 2026",
                "Solution complète et révolutionnaire intégrant tous nos services premium avec les dernières innovations 2026. Sécurisation garantie, immatriculation et validation du profil Google avec mises à jour automatiques.",
                "🚀",
                List.of(
                    "Tous les autres services inclus",
                    "Sécurisation garantie",
                    "Immatriculation du profil Google",
                    "Validation du profil Google",
                    "Mise à jour automatique du profil Google",
                    "Remise des accès propriétaire principal",
                    "Fusion des profils doubles"
                ),
                "1000,00 $",
                1000.00,
                "Paiement Unique",
                "Google Premium"
            ),
            new ServiceInfo(
                "Assistance Technique",
                "Support technique avancé avec intelligence artificielle 2026 pour dominer votre marché. Rapport détaillé sur les performances de votre entreprise et activation du numéro d'identification.",
                "🛠️",
                List.of(
                    "Rapport sur les performances de son entreprise sur Google",
                    "Intelligence artificielle 2026",
                    "Interactions avec les clients",
                    "Activation du numéro d'identification"
                ),
                "475,00 $",
                475.00,
                "Paiement Unique",
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
