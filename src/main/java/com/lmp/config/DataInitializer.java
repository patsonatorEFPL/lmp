package com.lmp.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.lmp.domain.entity.Role;
import com.lmp.domain.entity.Service;
import com.lmp.domain.entity.ServiceBenefit;
import com.lmp.domain.entity.ServiceCategory;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.domain.entity.User;
import com.lmp.domain.entity.enums.DurationType;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.RoleRepository;
import com.lmp.repository.ServiceCategoryRepository;
import com.lmp.repository.ServiceOfferRepository;
import com.lmp.repository.ServiceRepository;
import com.lmp.repository.UserRepository;

/**
 * Initialisateur de données pour l'application LMP.
 * 
 * Crée les données essentielles au démarrage de l'application
 * si elles n'existent pas déjà.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceOfferRepository serviceOfferRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 === DÉMARRAGE INITIALISATION DONNÉES LMP ===");
        
        initializeRoles();
        initializeDefaultUsers();
        initializeServices();
        
        // Diagnostic des données initialisées
        logger.info("📊 Nombre total de rôles: {}", roleRepository.count());
        logger.info("👥 Nombre total d'utilisateurs: {}", userRepository.count());
        logger.info("📦 Nombre total de catégories de services: {}", serviceCategoryRepository.count());
        logger.info("🛒 Nombre total de services: {}", serviceRepository.count());
        
        // Vérification du compte administrateur
        userRepository.findByEmail("admin@lmp.ca").ifPresentOrElse(
            admin -> logger.info("✅ Compte administrateur configuré: {}", admin.getEmail()),
            () -> logger.error("❌ Erreur: Compte administrateur non trouvé!")
        );
        
        logger.info("🏁 === Initialisation des données LMP terminée ===");
    }

    /**
     * Initialise les rôles par défaut s'ils n'existent pas.
     */
    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            System.out.println("Création des rôles par défaut...");
            
            Role userRole = new Role("USER");
            Role adminRole = new Role("ADMIN");
            
            roleRepository.save(userRole);
            roleRepository.save(adminRole);
            
            System.out.println("✓ Rôles créés: USER, ADMIN");
        }
    }

    /**
     * Initialise les utilisateurs par défaut s'ils n'existent pas.
     */
    private void initializeDefaultUsers() {
        // Créer l'administrateur par défaut
        if (!userRepository.existsByEmail("admin@lmp.ca")) {
            System.out.println("Création de l'utilisateur administrateur...");
            
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
            
            User admin = new User();
            admin.setEmail("admin@lmp.ca");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("LMP");
            admin.setRegistrationDate(LocalDateTime.now());
            admin.setStatus(UserStatus.ACTIVE);
            admin.setAccountLocked(false);
            admin.setEmailVerified(true);
            admin.setRoles(Set.of(adminRole));
            
            userRepository.save(admin);
            System.out.println("✓ Administrateur créé: admin@lmp.ca");
        }

        System.out.println("✓ Utilisateur administrateur configuré");
    }

    /**
     * Initialise les services, catégories et offres par défaut s'ils n'existent pas.
     */
    private void initializeServices() {
        if (serviceCategoryRepository.count() > 0) {
            logger.info("✅ Services déjà initialisés, skip.");
            return;
        }

        logger.info("🔧 Création des catégories, services et offres par défaut...");

        // Categories
        ServiceCategory refLocal = serviceCategoryRepository
                .save(new ServiceCategory("Référencement Local", "referencement-local", "", "📍", 1));
        ServiceCategory refPremium = serviceCategoryRepository
                .save(new ServiceCategory("Référencement Premium", "referencement-premium", "", "⭐", 2));
        ServiceCategory eReputation = serviceCategoryRepository
                .save(new ServiceCategory("Réputation en Ligne", "reputation-en-ligne", "", "💬", 3));
        ServiceCategory devWeb = serviceCategoryRepository
                .save(new ServiceCategory("Développement Web", "developpement-web", "", "💻", 4));
        ServiceCategory seoAvance = serviceCategoryRepository
                .save(new ServiceCategory("SEO Avancé", "seo-avance", "", "🔍", 5));
        ServiceCategory googlePrem = serviceCategoryRepository
                .save(new ServiceCategory("Google Premium", "google-premium", "", "🚀", 6));
        ServiceCategory support = serviceCategoryRepository
                .save(new ServiceCategory("Support", "support", "", "🛠️", 7));
        ServiceCategory marketingLocal = serviceCategoryRepository
                .save(new ServiceCategory("Marketing Local", "marketing-local", "", "🏪", 8));
        ServiceCategory offreSpeciale = serviceCategoryRepository
                .save(new ServiceCategory("Offre Spéciale", "offre-speciale", "", "🎁", 9));

        // Services + Default Offers
        createServiceWithOffer(refLocal, "Sécurisations et Accès Google My Business", "securisations-acces-gmb",
                "Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti.",
                "📍", 1, true, Arrays.asList(
                        "Remise des accès propriétaire principal",
                        "Synchronisation du profil Google avec votre courriel",
                        "Sécurisation du Google My Business"),
                new BigDecimal("353.89"));

        createServiceWithOffer(refPremium, "Référencement Optimale avec Sécurisations Garantie VIP+", "ref-optimale-vip",
                "Service premium exclusif avec garantie de résultats exceptionnels.",
                "⭐", 2, true, Arrays.asList(
                        "8 mots clés garantis",
                        "8 zones de services",
                        "Remise et sécurisation des accès garanti",
                        "Remise des accès propriétaire principal",
                        "Validation du profil Google"),
                new BigDecimal("750.79"));

        createServiceWithOffer(eReputation, "Gestion des Avis", "gestion-avis",
                "Améliorez votre réputation en ligne avec notre service de gestion des avis.",
                "💬", 3, true, Arrays.asList(
                        "Soyez le mieux noté de votre secteur",
                        "Gérez les avis indésirables",
                        "Recevez plus d'avis positifs"),
                new BigDecimal("747.43"));

        createServiceWithOffer(marketingLocal, "Présence Locales", "presence-locales",
                "Boostez votre visibilité locale avec une présence optimisée dans tous les annuaires.",
                "🏪", 4, true, Arrays.asList(
                        "Montez dans les résultats de la carte Google",
                        "Boostez votre visibilité dans votre ville",
                        "Visible dans tous les annuaires locaux"),
                new BigDecimal("2200.00"));

        createServiceWithOffer(devWeb, "Création Site Web", "creation-site-web",
                "Création de site web professionnel avec protocole SSL, référencement optimisé et promotion incluse.",
                "💻", 5, true, Arrays.asList(
                        "Création + promotion avec protocole SSL",
                        "Référencement parmi les meilleurs résultats",
                        "Boost des pages (publicité)"),
                new BigDecimal("550.00"));

        createServiceWithOffer(seoAvance, "SEO et Référencement Naturel", "seo-naturel",
                "Service SEO complet pour améliorer votre positionnement sur les moteurs de recherche.",
                "🔍", 6, true, Arrays.asList(
                        "Améliorez votre positionnement sur les moteurs",
                        "Mise à jour régulière des données GMB",
                        "Résultats garantis pour Google Maps"),
                new BigDecimal("5500.00"));

        createServiceWithOffer(googlePrem, "Mise à jour 2026", "mise-jour-2026",
                "Solution complète intégrant tous nos services premium avec les dernières innovations 2026.",
                "🚀", 7, true, Arrays.asList(
                        "Tous les autres services inclus",
                        "Sécurisation garantie",
                        "Immatriculation du profil Google",
                        "Validation du profil Google",
                        "Mise à jour automatique du profil Google",
                        "Remise des accès propriétaire principal",
                        "Fusion des profils doubles"),
                new BigDecimal("1000.00"));

        createServiceWithOffer(support, "Assistance Technique", "assistance-technique",
                "Support technique avancé avec intelligence artificielle 2026 pour dominer votre marché.",
                "🛠️", 8, false, Arrays.asList(
                        "Rapport sur les performances de son entreprise sur Google",
                        "Intelligence artificielle 2026",
                        "Interactions avec les clients",
                        "Activation du numéro d'identification"),
                new BigDecimal("475.00"));

        createServiceWithOffer(offreSpeciale, "Black Friday : Authentification, Gestion des avis et Mise à jour map", "black-friday",
                "Pack complet incluant l'authentification, la gestion des avis et la mise à jour de votre carte Google Maps.",
                "🎁", 9, false, Arrays.asList(
                        "Authentification sécurisée",
                        "Gestion des avis clients",
                        "Mise à jour Google Maps"),
                new BigDecimal("600.00"));

        logger.info("✅ {} catégories et {} services créés avec leurs offres.",
                serviceCategoryRepository.count(), serviceRepository.count());
    }

    private void createServiceWithOffer(ServiceCategory category, String title, String slug,
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
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());

        for (String b : benefits) {
            ServiceBenefit benefit = new ServiceBenefit();
            benefit.setBenefit(b);
            service.addBenefit(benefit);
        }

        service = serviceRepository.save(service);

        ServiceOffer offer = new ServiceOffer(service, "Tarif Standard", price, null, DurationType.ONE_TIME, true);
        serviceOfferRepository.save(offer);
    }
}