package com.lmp.shared.config;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.lmp.auth.domain.Role;
import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceBenefit;
import com.lmp.catalog.domain.ServiceCategory;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.catalog.domain.OfferBenefit;
import com.lmp.auth.domain.User;
import com.lmp.catalog.domain.DurationType;
import com.lmp.auth.domain.UserStatus;
import com.lmp.catalog.repository.OfferBenefitRepository;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.catalog.repository.ServiceCategoryRepository;
import com.lmp.catalog.repository.ServiceOfferRepository;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.auth.repository.UserRepository;

/**
 * Initialisateur de données pour l'application LMP.
 *
 * Idempotent : chaque entité est créée par slug/nom seulement si elle est absente.
 * Les redéploiements ne suppriment ni n'écrasent les données existantes.
 * Pour ajouter un nouveau service, ajouter son entrée ici → il sera créé au prochain déploiement.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final OfferBenefitRepository offerBenefitRepository;

    /**
     * Mot de passe admin — la variable d'environnement est prioritaire sur la DB.
     *
     * Règles :
     *  • ENV présente → écrase le hash en base si différent (rotation/reset).
     *  • ENV absente + admin existe → conserve le hash persisté en DB.
     *  • ENV absente + admin absent → IllegalStateException (bootstrap initial obligatoire).
     */
    @org.springframework.beans.factory.annotation.Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                           ServiceCategoryRepository serviceCategoryRepository,
                           ServiceRepository serviceRepository,
                           ServiceOfferRepository serviceOfferRepository,
                           OfferBenefitRepository offerBenefitRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.serviceRepository = serviceRepository;
        this.serviceOfferRepository = serviceOfferRepository;
        this.offerBenefitRepository = offerBenefitRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 === DÉMARRAGE INITIALISATION DONNÉES LMP ===");

        initializeRoles();
        initializeDefaultUsers();
        initializeServices();

        logger.info("📊 Nombre total de rôles: {}", roleRepository.count());
        logger.info("👥 Nombre total d'utilisateurs: {}", userRepository.count());
        logger.info("📦 Nombre total de catégories de services: {}", serviceCategoryRepository.count());
        logger.info("🛒 Nombre total de services: {}", serviceRepository.count());

        userRepository.findByEmail("admin@lmp.ca").ifPresentOrElse(
            admin -> logger.info("✅ Compte administrateur configuré: {}", admin.getEmail()),
            () -> logger.error("❌ Erreur: Compte administrateur non trouvé!")
        );

        logger.info("🏁 === Initialisation des données LMP terminée ===");
    }

    // -------------------------------------------------------------------------
    // Rôles
    // -------------------------------------------------------------------------

    private void initializeRoles() {
        createRoleIfMissing("USER");
        createRoleIfMissing("ADMIN");
    }

    private void createRoleIfMissing(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(name));
            logger.info("✅ Rôle créé : {}", name);
        }
    }

    // -------------------------------------------------------------------------
    // Utilisateurs par défaut
    // -------------------------------------------------------------------------

    private void initializeDefaultUsers() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));

        boolean envProvided = adminPassword != null && !adminPassword.isBlank();
        var existingAdmin = userRepository.findByEmail("admin@lmp.ca");

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            if (envProvided) {
                // ENV prioritaire : synchronise le hash en base si différent
                if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    userRepository.save(admin);
                    logger.info("🔄 Mot de passe admin mis à jour depuis ADMIN_PASSWORD.");
                } else {
                    logger.debug("📋 Compte admin existant — mot de passe déjà synchronisé avec ADMIN_PASSWORD.");
                }
            } else {
                logger.info("📋 ADMIN_PASSWORD non défini — conservation du mot de passe persisté en base.");
            }
            return;
        }

        if (!envProvided) {
            throw new IllegalStateException(
                "❌ Aucun compte admin en base et ADMIN_PASSWORD non défini. "
                + "Définissez la variable d'environnement ADMIN_PASSWORD pour le bootstrap initial.");
        }

        User admin = new User();
        admin.setEmail("admin@lmp.ca");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstName("Admin");
        admin.setLastName("LMP");
        admin.setRegistrationDate(LocalDateTime.now());
        admin.setStatus(UserStatus.ACTIVE);
        admin.setAccountLocked(false);
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);
        logger.info("✅ Administrateur créé : admin@lmp.ca");
    }

    /**
     * Génère un mot de passe aléatoire sécurisé (même approche que frappe.utils.password).
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Services — idempotent par slug
    // -------------------------------------------------------------------------

    /**
     * Chaque catégorie et service est créé seulement s'il est absent (lookup par slug).
     * Ajouter une nouvelle entrée ici suffit pour qu'elle apparaisse au prochain déploiement,
     * même si la base de données contient déjà d'autres services.
     */
    private void initializeServices() {
        logger.info("🔧 Vérification et création des services manquants...");

        ServiceCategory refLocal      = getOrCreateCategory("Référencement Local",    "referencement-local",    "📍", 1);
        ServiceCategory refPremium    = getOrCreateCategory("Référencement Premium",  "referencement-premium",  "⭐", 2);
        ServiceCategory eReputation   = getOrCreateCategory("Réputation en Ligne",    "reputation-en-ligne",    "💬", 3);
        ServiceCategory devWeb        = getOrCreateCategory("Développement Web",      "developpement-web",      "💻", 4);
        ServiceCategory seoAvance     = getOrCreateCategory("SEO Avancé",             "seo-avance",             "🔍", 5);
        ServiceCategory googlePrem    = getOrCreateCategory("Google Premium",         "google-premium",         "🚀", 6);
        ServiceCategory support       = getOrCreateCategory("Support",                "support",                "🛠️", 7);
        ServiceCategory marketingLocal = getOrCreateCategory("Marketing Local",       "marketing-local",        "🏪", 8);
        ServiceCategory offreSpeciale = getOrCreateCategory("Offre Spéciale",         "offre-speciale",         "🎁", 9);

        createServiceIfMissing(refLocal, "Sécurisations et Accès Google My Business", "securisations-acces-gmb",
                "Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti. Synchronisation complète du profil avec votre courriel professionnel et sécurisation avancée contre toute modification non autorisée.",
                "📍", 1, true,
                Arrays.asList(
                        "Remise des accès propriétaire principal",
                        "Synchronisation du profil Google avec votre courriel",
                        "Sécurisation du Google My Business"),
                new BigDecimal("353.89"));

        createServiceIfMissing(refPremium, "Référencement Optimale avec Sécurisations Garantie VIP+", "ref-optimale-vip",
                "Service premium exclusif avec garantie de résultats exceptionnels. Bénéficiez de 8 mots clés stratégiques et 8 zones de services optimisées pour dominer votre marché local.",
                "⭐", 2, true,
                Arrays.asList(
                        "8 mots clés garantis",
                        "8 zones de services",
                        "Remise et sécurisation des accès garanti",
                        "Remise des accès propriétaire principal",
                        "Validation du profil Google"),
                new BigDecimal("750.79"));

        createServiceIfMissing(eReputation, "Gestion des Avis", "gestion-avis",
                "Améliorez votre réputation en ligne avec notre service de gestion des avis. Multipliez les avis positifs et gérez efficacement les commentaires.",
                "💬", 3, true,
                Arrays.asList(
                        "Soyez le mieux noté de votre secteur",
                        "Gérez les avis indésirables",
                        "Recevez plus d'avis positifs"),
                new BigDecimal("747.43"));

        createServiceIfMissing(marketingLocal, "Présence Locales", "presence-locales",
                "Boostez votre visibilité locale avec une présence optimisée dans tous les annuaires locaux et sur Google Maps.",
                "🏪", 4, true,
                Arrays.asList(
                        "Montez dans les résultats de la carte Google",
                        "Boostez votre visibilité dans votre ville",
                        "Visible dans tous les annuaires locaux"),
                new BigDecimal("2200.00"));

        createServiceIfMissing(devWeb, "Création Site Web", "creation-site-web",
                "Création de site web professionnel avec protocole SSL, référencement optimisé et promotion incluse.",
                "💻", 5, true,
                Arrays.asList(
                        "Création + promotion avec protocole SSL",
                        "Référencement parmi les meilleurs résultats",
                        "Boost des pages (publicité)"),
                new BigDecimal("550.00"));

        createServiceIfMissing(seoAvance, "SEO et Référencement Naturel", "seo-naturel",
                "Service SEO complet pour améliorer votre positionnement sur les moteurs de recherche et Google Maps.",
                "🔍", 6, true,
                Arrays.asList(
                        "Améliorez votre positionnement sur les moteurs",
                        "Mise à jour régulière des données GMB",
                        "Résultats garantis pour Google Maps"),
                new BigDecimal("5500.00"));

        createServiceIfMissing(googlePrem, "Mise à jour 2026", "mise-jour-2026",
                "Solution complète et révolutionnaire intégrant tous nos services premium avec les dernières innovations 2026. Sécurisation garantie, immatriculation et validation du profil Google avec mises à jour automatiques.",
                "🚀", 7, true,
                Arrays.asList(
                        "Tous les autres services inclus",
                        "Sécurisation garantie",
                        "Immatriculation du profil Google",
                        "Validation du profil Google",
                        "Mise à jour automatique du profil Google",
                        "Remise des accès propriétaire principal",
                        "Fusion des profils doubles"),
                new BigDecimal("1000.00"));

        createServiceIfMissing(support, "Assistance Technique", "assistance-technique",
                "Support technique avancé avec intelligence artificielle 2026 pour dominer votre marché. Rapport détaillé sur les performances de votre entreprise et activation du numéro d'identification.",
                "🛠️", 8, false,
                Arrays.asList(
                        "Rapport sur les performances de son entreprise sur Google",
                        "Intelligence artificielle 2026",
                        "Interactions avec les clients",
                        "Activation du numéro d'identification"),
                new BigDecimal("475.00"));

        createServiceIfMissing(offreSpeciale, "Black Friday : Authentification, Gestion des avis et Mise à jour map", "black-friday",
                "Pack complet incluant l'authentification, la gestion des avis et la mise à jour de votre carte Google Maps.",
                "🎁", 9, false,
                Arrays.asList(
                        "Authentification sécurisée",
                        "Gestion des avis clients",
                        "Mise à jour Google Maps"),
                new BigDecimal("600.00"));

        logger.info("✅ Vérification des services terminée — {} catégories, {} services en base.",
                serviceCategoryRepository.count(), serviceRepository.count());
    }

    // -------------------------------------------------------------------------
    // Helpers idempotents
    // -------------------------------------------------------------------------

    /**
     * Retourne la catégorie existante par slug, ou la crée si absente.
     */
    private ServiceCategory getOrCreateCategory(String name, String slug, String icon, int order) {
        return serviceCategoryRepository.findBySlug(slug).orElseGet(() -> {
            logger.info("➕ Création catégorie : {}", name);
            return serviceCategoryRepository.save(new ServiceCategory(name, slug, "", icon, order));
        });
    }

    /**
     * Crée le service (+ son offre par défaut) seulement si le slug est absent.
     * N'écrase pas les services modifiés manuellement en base.
     */
    private void createServiceIfMissing(ServiceCategory category, String title, String slug,
            String description, String icon, int order, boolean featured,
            List<String> benefits, BigDecimal price) {

        if (serviceRepository.findBySlug(slug).isPresent()) {
            logger.debug("⏭️  Service '{}' déjà présent, ignoré.", slug);
            return;
        }

        logger.info("➕ Création service : {}", title);

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
        offer = serviceOfferRepository.save(offer);

        for (int i = 0; i < benefits.size(); i++) {
            OfferBenefit ob = new OfferBenefit();
            ob.setOffer(offer);
            ob.setBenefit(benefits.get(i));
            ob.setDisplayOrder(i);
            offerBenefitRepository.save(ob);
        }
    }
}
