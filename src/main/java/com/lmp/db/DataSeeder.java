package com.lmp.db;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.lmp.domain.entity.Service;
import com.lmp.domain.entity.ServiceBenefit;
import com.lmp.domain.entity.ServiceCategory;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.domain.entity.enums.DurationType;
import com.lmp.repository.ServiceCategoryRepository;
import com.lmp.repository.ServiceOfferRepository;
import com.lmp.repository.ServiceRepository;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceOfferRepository offerRepository;

    public DataSeeder(ServiceCategoryRepository categoryRepository,
            ServiceRepository serviceRepository,
            ServiceOfferRepository offerRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            seedData();
        }
    }

    private void seedData() {
        // 1. Categories
        ServiceCategory refLocal = categoryRepository
                .save(new ServiceCategory("Référencement Local", "referencement-local", "", "📍", 1));
        ServiceCategory refPremium = categoryRepository
                .save(new ServiceCategory("Référencement Premium", "referencement-premium", "", "⭐", 2));
        ServiceCategory eReputation = categoryRepository
                .save(new ServiceCategory("Réputation en Ligne", "reputation-en-ligne", "", "💬", 3));
        ServiceCategory devWeb = categoryRepository
                .save(new ServiceCategory("Développement Web", "developpement-web", "", "💻", 4));
        ServiceCategory seoAvance = categoryRepository
                .save(new ServiceCategory("SEO Avancé", "seo-avance", "", "🔍", 5));
        ServiceCategory googlePrem = categoryRepository
                .save(new ServiceCategory("Google Premium", "google-premium", "", "🚀", 6));
        ServiceCategory support = categoryRepository.save(new ServiceCategory("Support", "support", "", "🛠️", 7));
        ServiceCategory marketingLocal = categoryRepository
                .save(new ServiceCategory("Marketing Local", "marketing-local", "", "🏪", 8));
        ServiceCategory offreSpeciale = categoryRepository
                .save(new ServiceCategory("Offre Spéciale", "offre-speciale", "", "🎁", 9));

        // 2. Services & Benefits & Default Offers

        createServiceAndOffer(
                refLocal, "Sécurisations et Accès Google My Business", "securisations-acces-gmb",
                "Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti. Synchronisation complète du profil avec votre courriel professionnel et sécurisation avancée contre toute modification non autorisée.",
                "📍", 1, true, Arrays.asList(
                        "Remise des accès propriétaire principal",
                        "Synchronisation du profil Google avec votre courriel",
                        "Sécurisation du Google My Business"),
                new BigDecimal("353.89"));

        createServiceAndOffer(
                refPremium, "Référencement Optimale avec Sécurisations Garantie VIP+", "ref-optimale-vip",
                "Service premium exclusif avec garantie de résultats exceptionnels. Bénéficiez de 8 mots clés stratégiques et 8 zones de services optimisées pour dominer votre marché local.",
                "⭐", 2, true, Arrays.asList(
                        "8 mots clés garantis",
                        "8 zones de services",
                        "Remise et sécurisation des accès garanti",
                        "Remise des accès propriétaire principal",
                        "Validation du profil Google"),
                new BigDecimal("750.79"));

        createServiceAndOffer(
                eReputation, "Gestion des Avis", "gestion-avis",
                "Améliorez votre réputation en ligne avec notre service de gestion des avis. Multipliez les avis positifs et gérez efficacement les commentaires.",
                "💬", 3, true, Arrays.asList(
                        "Soyez le mieux noté de votre secteur",
                        "Gérez les avis indésirables",
                        "Recevez plus d'avis positifs"),
                new BigDecimal("747.43"));

        createServiceAndOffer(
                marketingLocal, "Présence Locales", "presence-locales",
                "Boostez votre visibilité locale avec une présence optimisée dans tous les annuaires locaux et sur Google Maps.",
                "🏪", 4, true, Arrays.asList(
                        "Montez dans les résultats de la carte Google",
                        "Boostez votre visibilité dans votre ville",
                        "Visible dans tous les annuaires locaux"),
                new BigDecimal("2200.00"));

        createServiceAndOffer(
                devWeb, "Création Site Web", "creation-site-web",
                "Création de site web professionnel avec protocole SSL, référencement optimisé et promotion incluse.",
                "💻", 5, true, Arrays.asList(
                        "Création + promotion avec protocole SSL",
                        "Référencement parmi les meilleurs résultats",
                        "Boost des pages (publicité)"),
                new BigDecimal("550.00"));

        createServiceAndOffer(
                seoAvance, "SEO et Référencement Naturel", "seo-naturel",
                "Service SEO complet pour améliorer votre positionnement sur les moteurs de recherche et Google Maps.",
                "🔍", 6, true, Arrays.asList(
                        "Améliorez votre positionnement sur les moteurs",
                        "Mise à jour régulière des données GMB",
                        "Résultats garantis pour Google Maps"),
                new BigDecimal("5500.00"));

        createServiceAndOffer(
                googlePrem, "Mise à jour 2026", "mise-jour-2026",
                "Solution complète et révolutionnaire intégrant tous nos services premium avec les dernières innovations 2026. Sécurisation garantie, immatriculation et validation du profil Google avec mises à jour automatiques.",
                "🚀", 7, true, Arrays.asList(
                        "Tous les autres services inclus",
                        "Sécurisation garantie",
                        "Immatriculation du profil Google",
                        "Validation du profil Google",
                        "Mise à jour automatique du profil Google",
                        "Remise des accès propriétaire principal",
                        "Fusion des profils doubles"),
                new BigDecimal("1000.00"));

        createServiceAndOffer(
                support, "Assistance Technique", "assistance-technique",
                "Support technique avancé avec intelligence artificielle 2026 pour dominer votre marché. Rapport détaillé sur les performances de votre entreprise et activation du numéro d'identification.",
                "🛠️", 8, false, Arrays.asList(
                        "Rapport sur les performances de son entreprise sur Google",
                        "Intelligence artificielle 2026",
                        "Interactions avec les clients",
                        "Activation du numéro d'identification"),
                new BigDecimal("475.00"));

        Service blackFridaySvc = createServiceAndOffer(
                offreSpeciale, "Black Friday : Authentification, Gestion des avis et Mise à jour map", "black-friday",
                "Pack complet incluant l'authentification, la gestion des avis et la mise à jour de votre carte Google Maps.",
                "🎁", 9, false, Arrays.asList(
                        "Authentification sécurisée",
                        "Gestion des avis clients",
                        "Mise à jour Google Maps"),
                new BigDecimal("600.00"));

        // Example: Add a promotional offer for the first service to test the new system
        ServiceOffer promoOffer = new ServiceOffer();
        promoOffer.setService(serviceRepository.findByTitle("Sécurisations et Accès Google My Business").orElseThrow());
        promoOffer.setName("Promo Flash - 24H");
        promoOffer.setPrice(new BigDecimal("299.99"));
        promoOffer.setOriginalPrice(new BigDecimal("353.89"));
        promoOffer.setDurationType(DurationType.ONE_TIME);
        // Valid for 24h from now
        promoOffer.setValidFrom(java.time.LocalDateTime.now().minusHours(1));
        promoOffer.setValidTo(java.time.LocalDateTime.now().plusHours(24));
        promoOffer.setIsDefault(false);
        promoOffer.setActive(true);
        offerRepository.save(promoOffer);
    }

    private Service createServiceAndOffer(ServiceCategory category, String title, String slug,
            String description, String icon, int order, boolean featured,
            List<String> benefits, BigDecimal price) {
        Service service = new Service();
        service.setCategory(category);
        service.setTitle(title);
        service.setSlug(slug);
        service.setDescription(description);
        service.setIcon(icon);
        service.setDisplayOrder(order);
        service.setActive(true);
        service.setFeatured(featured);

        for (String b : benefits) {
            ServiceBenefit benefit = new ServiceBenefit();
            benefit.setBenefit(b);
            service.addBenefit(benefit);
        }

        service = serviceRepository.save(service);

        ServiceOffer offer = new ServiceOffer();
        offer.setService(service);
        offer.setName("Tarif Standard");
        offer.setPrice(price);
        offer.setDurationType(DurationType.ONE_TIME);
        offer.setIsDefault(true);
        offer.setActive(true);
        offerRepository.save(offer);

        return service;
    }
}
