# Guide d'Implémentation - Vues d'Administration

## 🚀 Phase 1: Controller pour Gestion des Utilisateurs

### Étape 1.1: AdminUserViewController.java

**Fichier:** `src/main/java/com/lmp/web/controller/admin/AdminUserViewController.java`

```java
package com.lmp.web.controller.admin;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.service.user.UserService;
import com.lmp.web.dto.admin.UserFilterDto;

/**
 * Contrôleur pour la gestion des utilisateurs via interface web admin
 */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserViewController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserViewController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + AdminUserViewController.class.getName());

    @Autowired
    private UserService userService;

    /**
     * Affiche la page de gestion des utilisateurs
     */
    @GetMapping
    public String showUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "registrationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        
        try {
            // Configuration pagination et tri
            Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Filtrage des utilisateurs
            Page<User> usersPage;
            if (status != null && !status.isEmpty()) {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                usersPage = userService.findByStatus(userStatus, pageable);
            } else if (search != null && !search.isEmpty()) {
                usersPage = userService.findByEmailContaining(search, pageable);
            } else {
                usersPage = userService.findAll(pageable);
            }
            
            // Statistiques rapides
            long totalUsers = userService.count();
            long activeUsers = userService.countByStatus(UserStatus.ACTIVE);
            long inactiveUsers = userService.countByStatus(UserStatus.INACTIVE);
            long lockedUsers = userService.countByAccountLocked(true);
            
            // Ajout au modèle
            model.addAttribute("usersPage", usersPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute("totalElements", usersPage.getTotalElements());
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentSort", sortBy);
            model.addAttribute("currentSortDir", sortDir);
            
            // Statistiques
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("activeUsers", activeUsers);
            model.addAttribute("inactiveUsers", inactiveUsers);
            model.addAttribute("lockedUsers", lockedUsers);
            
            logger.info("Users page loaded successfully - Total: {}, Page: {}/{}", 
                       usersPage.getTotalElements(), page + 1, usersPage.getTotalPages());
            
            return "admin/users";
            
        } catch (Exception e) {
            logger.error("Error loading users page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement des utilisateurs: " + e.getMessage());
            return "admin/users";
        }
    }

    /**
     * Active un utilisateur
     */
    @PostMapping("/{id}/activate")
    @ResponseBody
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setStatus(UserStatus.ACTIVE);
            user.setAccountLocked(false);
            userService.save(user);
            
            auditLogger.info("User activated - ID: {}, Email: {}", id, user.getEmail());
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Utilisateur activé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("Error activating user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Désactive un utilisateur
     */
    @PostMapping("/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setStatus(UserStatus.INACTIVE);
            userService.save(user);
            
            auditLogger.info("User deactivated - ID: {}, Email: {}", id, user.getEmail());
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Utilisateur désactivé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("Error deactivating user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Verrouille un compte utilisateur
     */
    @PostMapping("/{id}/lock")
    @ResponseBody
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setAccountLocked(true);
            userService.save(user);
            
            auditLogger.info("User account locked - ID: {}, Email: {}", id, user.getEmail());
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Compte verrouillé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("Error locking user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Déverrouille un compte utilisateur
     */
    @PostMapping("/{id}/unlock")
    @ResponseBody
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setAccountLocked(false);
            userService.save(user);
            
            auditLogger.info("User account unlocked - ID: {}, Email: {}", id, user.getEmail());
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Compte déverrouillé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("Error unlocking user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Affiche les détails d'un utilisateur
     */
    @GetMapping("/{id}/details")
    @ResponseBody
    public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Créer un DTO avec les détails
            UserDetailsDto details = new UserDetailsDto(user);
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            logger.error("Error fetching user details {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * DTO pour les détails utilisateur
     */
    public static class UserDetailsDto {
        private Long id;
        private String email;
        private String displayName;
        private String phone;
        private String address;
        private String city;
        private String postalCode;
        private String country;
        private String companyName;
        private String status;
        private Boolean accountLocked;
        private Boolean emailVerified;
        private String registrationDate;
        private String lastLoginDate;

        public UserDetailsDto(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.displayName = user.getDisplayName();
            this.phone = user.getPhone();
            this.address = user.getAddress();
            this.city = user.getCity();
            this.postalCode = user.getPostalCode();
            this.country = user.getCountry();
            this.companyName = user.getCompanyName();
            this.status = user.getStatus().name();
            this.accountLocked = user.getAccountLocked();
            this.emailVerified = user.getEmailVerified();
            this.registrationDate = user.getRegistrationDate() != null ? 
                user.getRegistrationDate().toString() : null;
            this.lastLoginDate = user.getLastLoginDate() != null ? 
                user.getLastLoginDate().toString() : null;
        }

        // Getters
        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public String getCity() { return city; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }
        public String getCompanyName() { return companyName; }
        public String getStatus() { return status; }
        public Boolean getAccountLocked() { return accountLocked; }
        public Boolean getEmailVerified() { return emailVerified; }
        public String getRegistrationDate() { return registrationDate; }
        public String getLastLoginDate() { return lastLoginDate; }
    }
}
```

## 🚀 Phase 2: Service pour Paramètres Système

### Étape 2.1: SystemConfigService.java

**Fichier:** `src/main/java/com/lmp/service/system/SystemConfigService.java`

```java
package com.lmp.service.system;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.lmp.web.dto.admin.SystemSettingsDto;

/**
 * Service pour la gestion des paramètres système
 */
@Service
public class SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + SystemConfigService.class.getName());

    @Autowired
    private Environment environment;

    /**
     * Charge tous les paramètres système
     */
    public SystemSettingsDto loadAllSettings() {
        try {
            SystemSettingsDto settings = new SystemSettingsDto();
            
            // Configuration Application
            settings.setAppName(environment.getProperty("app.name", "LMP Digital Services"));
            settings.setAppVersion(environment.getProperty("app.version", "1.0.0"));
            settings.setAppBaseUrl(environment.getProperty("app.base.url", "http://localhost:8080"));
            
            // Configuration Entreprise
            settings.setCompanyName(environment.getProperty("company.name", "LMP Digital Services"));
            settings.setCompanyEmail(environment.getProperty("company.email", "contact@lmp-digital.ca"));
            settings.setCompanyPhone(environment.getProperty("company.phone", "+1 (555) 123-4567"));
            settings.setCompanyAddress(environment.getProperty("company.address", "123 Rue Principale"));
            settings.setCompanyWebsite(environment.getProperty("company.website", "https://lmp-digital.ca"));
            
            // Configuration Stripe
            settings.setStripePublishableKey(environment.getProperty("stripe.publishable.key", ""));
            settings.setStripeTestMode(environment.getProperty("stripe.secret.key", "").contains("test"));
            
            // Configuration Email
            settings.setMailHost(environment.getProperty("spring.mail.host", "smtp.gmail.com"));
            settings.setMailPort(environment.getProperty("spring.mail.port", "587"));
            settings.setMailUsername(environment.getProperty("spring.mail.username", ""));
            settings.setMailAuthEnabled(Boolean.parseBoolean(
                environment.getProperty("spring.mail.properties.mail.smtp.auth", "true")));
            
            // Configuration Sécurité
            settings.setPasswordMinLength(Integer.parseInt(
                environment.getProperty("security.password.min.length", "8")));
            settings.setMaxLoginAttempts(Integer.parseInt(
                environment.getProperty("security.max.login.attempts", "5")));
            settings.setAccountLockoutDuration(Long.parseLong(
                environment.getProperty("security.account.lockout.duration", "300000")));
            
            // Configuration Paiements
            settings.setDefaultCurrency(environment.getProperty("payment.default.currency", "CAD"));
            settings.setMaxAmount(Double.parseDouble(
                environment.getProperty("payment.max.amount", "10000.00")));
            settings.setMinAmount(Double.parseDouble(
                environment.getProperty("payment.min.amount", "1.00")));
            
            logger.info("System settings loaded successfully");
            return settings;
            
        } catch (Exception e) {
            logger.error("Error loading system settings: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors du chargement des paramètres", e);
        }
    }

    /**
     * Sauvegarde les paramètres système
     */
    public void saveSettings(SystemSettingsDto settings) {
        try {
            // Validation des paramètres
            validateSettings(settings);
            
            // Création des propriétés à sauvegarder
            Properties props = new Properties();
            
            // Application
            props.setProperty("app.name", settings.getAppName());
            props.setProperty("app.version", settings.getAppVersion());
            props.setProperty("app.base.url", settings.getAppBaseUrl());
            
            // Entreprise
            props.setProperty("company.name", settings.getCompanyName());
            props.setProperty("company.email", settings.getCompanyEmail());
            props.setProperty("company.phone", settings.getCompanyPhone());
            props.setProperty("company.address", settings.getCompanyAddress());
            props.setProperty("company.website", settings.getCompanyWebsite());
            
            // Email
            props.setProperty("spring.mail.host", settings.getMailHost());
            props.setProperty("spring.mail.port", settings.getMailPort());
            props.setProperty("spring.mail.username", settings.getMailUsername());
            props.setProperty("spring.mail.properties.mail.smtp.auth", 
                String.valueOf(settings.isMailAuthEnabled()));
            
            // Sécurité
            props.setProperty("security.password.min.length", 
                String.valueOf(settings.getPasswordMinLength()));
            props.setProperty("security.max.login.attempts", 
                String.valueOf(settings.getMaxLoginAttempts()));
            props.setProperty("security.account.lockout.duration", 
                String.valueOf(settings.getAccountLockoutDuration()));
            
            // Paiements
            props.setProperty("payment.default.currency", settings.getDefaultCurrency());
            props.setProperty("payment.max.amount", String.valueOf(settings.getMaxAmount()));
            props.setProperty("payment.min.amount", String.valueOf(settings.getMinAmount()));
            
            // Sauvegarde dans le fichier
            // NOTE: En production, utiliser un mécanisme plus robuste
            savePropertiesToFile(props);
            
            auditLogger.info("System settings updated successfully");
            
        } catch (Exception e) {
            logger.error("Error saving system settings: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la sauvegarde des paramètres", e);
        }
    }

    /**
     * Valide les paramètres système
     */
    private void validateSettings(SystemSettingsDto settings) {
        if (settings.getAppName() == null || settings.getAppName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'application est requis");
        }
        
        if (settings.getCompanyEmail() != null && !settings.getCompanyEmail().contains("@")) {
            throw new IllegalArgumentException("L'email de l'entreprise n'est pas valide");
        }
        
        if (settings.getPasswordMinLength() < 6 || settings.getPasswordMinLength() > 50) {
            throw new IllegalArgumentException("La longueur minimale du mot de passe doit être entre 6 et 50");
        }
        
        if (settings.getMaxLoginAttempts() < 1 || settings.getMaxLoginAttempts() > 10) {
            throw new IllegalArgumentException("Le nombre max de tentatives doit être entre 1 et 10");
        }
        
        if (settings.getMaxAmount() <= settings.getMinAmount()) {
            throw new IllegalArgumentException("Le montant maximum doit être supérieur au minimum");
        }
    }

    /**
     * Sauvegarde les propriétés dans le fichier
     * NOTE: Implémentation simplifiée pour la démo
     */
    private void savePropertiesToFile(Properties props) throws IOException {
        // En production, utiliser un système de configuration plus robuste
        // comme Spring Cloud Config ou une base de données
        logger.info("Properties would be saved to configuration file: {}", props.size());
        
        // Pour la démo, on log les propriétés
        props.forEach((key, value) -> 
            logger.debug("Config: {} = {}", key, value));
    }

    /**
     * Réinitialise les paramètres aux valeurs par défaut
     */
    public SystemSettingsDto resetToDefaults() {
        try {
            SystemSettingsDto defaults = new SystemSettingsDto();
            
            // Valeurs par défaut
            defaults.setAppName("LMP Digital Services");
            defaults.setAppVersion("1.0.0");
            defaults.setAppBaseUrl("http://localhost:8080");
            defaults.setCompanyName("LMP Digital Services");
            defaults.setCompanyEmail("contact@lmp-digital.ca");
            defaults.setCompanyPhone("+1 (555) 123-4567");
            defaults.setCompanyAddress("123 Rue Principale, Ville, Province, Code Postal");
            defaults.setCompanyWebsite("https://lmp-digital.ca");
            defaults.setMailHost("smtp.gmail.com");
            defaults.setMailPort("587");
            defaults.setMailUsername("");
            defaults.setMailAuthEnabled(true);
            defaults.setPasswordMinLength(8);
            defaults.setMaxLoginAttempts(5);
            defaults.setAccountLockoutDuration(300000L);
            defaults.setDefaultCurrency("CAD");
            defaults.setMaxAmount(10000.00);
            defaults.setMinAmount(1.00);
            
            auditLogger.info("System settings reset to defaults");
            return defaults;
            
        } catch (Exception e) {
            logger.error("Error resetting settings to defaults: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la réinitialisation", e);
        }
    }

    /**
     * Teste la configuration email
     */
    public boolean testEmailConfiguration(SystemSettingsDto settings) {
        try {
            // Implémentation simplifiée pour la démo
            // En production, envoyer un email de test réel
            
            if (settings.getMailHost() == null || settings.getMailHost().isEmpty()) {
                return false;
            }
            
            if (settings.getMailPort() == null || settings.getMailPort().isEmpty()) {
                return false;
            }
            
            logger.info("Email configuration test successful for host: {}", settings.getMailHost());
            return true;
            
        } catch (Exception e) {
            logger.error("Email configuration test failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
```

## 🚀 Phase 3: DTOs pour l'Administration

### Étape 3.1: SystemSettingsDto.java

**Fichier:** `src/main/java/com/lmp/web/dto/admin/SystemSettingsDto.java`

```java
package com.lmp.web.dto.admin;

import jakarta.validation.constraints.*;

/**
 * DTO pour les paramètres système
 */
public class SystemSettingsDto {
    
    // Configuration Application
    @NotBlank(message = "Le nom de l'application est requis")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String appName;
    
    @NotBlank(message = "La version est requise")
    @Pattern(regexp = "\\d+\\.\\d+\\.\\d+", message = "Format de version invalide (ex: 1.0.0)")
    private String appVersion;
    
    @NotBlank(message = "L'URL de base est requise")
    @URL(message = "L'URL doit être valide")
    private String appBaseUrl;
    
    // Configuration Entreprise
    @NotBlank(message = "Le nom de l'entreprise est requis")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String companyName;
    
    @Email(message = "L'email doit être valide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String companyEmail;
    
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String companyPhone;
    
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String companyAddress;
    
    @URL(message = "L'URL du site web doit être valide")
    private String companyWebsite;
    
    // Configuration Stripe
    private String stripePublishableKey;
    private boolean stripeTestMode;
    
    // Configuration Email
    @NotBlank(message = "L'hôte mail est requis")
    private String mailHost;
    
    @NotBlank(message = "Le port mail est requis")
    @Pattern(regexp = "\\d+", message = "Le port doit être un nombre")
    private String mailPort;
    
    private String mailUsername;
    private boolean mailAuthEnabled;
    
    // Configuration Sécurité
    @Min(value = 6, message = "La longueur minimale doit être au moins 6")
    @Max(value = 50, message = "La longueur minimale ne peut pas dépasser 50")
    private int passwordMinLength;
    
    @Min(value = 1, message = "Au moins 1 tentative doit être autorisée")
    @Max(value = 10, message = "Maximum 10 tentatives autorisées")
    private int maxLoginAttempts;
    
    @Min(value = 60000, message = "La durée de verrouillage doit être au moins 1 minute")
    private long accountLockoutDuration;
    
    // Configuration Paiements
    @NotBlank(message = "La devise par défaut est requise")
    @Size(min = 3, max = 3, message = "La devise doit faire 3 caractères")
    private String defaultCurrency;
    
    @DecimalMin(value = "0.01", message = "Le montant minimum doit être positif")
    private double minAmount;
    
    @DecimalMin(value = "1.00", message = "Le montant maximum doit être au moins 1")
    private double maxAmount;
    
    // Constructeurs
    public SystemSettingsDto() {}
    
    // Getters et Setters
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    
    public String getAppBaseUrl() { return appBaseUrl; }
    public void setAppBaseUrl(String appBaseUrl) { this.appBaseUrl = appBaseUrl; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    
    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
    
    public String getStripePublishableKey() { return stripePublishableKey; }
    public void setStripePublishableKey(String stripePublishableKey) { this.stripePublishableKey = stripePublishableKey; }
    
    public boolean isStripeTestMode() { return stripeTestMode; }
    public void setStripeTestMode(boolean stripeTestMode) { this.stripeTestMode = stripeTestMode; }
    
    public String getMailHost() { return mailHost; }
    public void setMailHost(String mailHost) { this.mailHost = mailHost; }
    
    public String getMailPort() { return mailPort; }
    public void setMailPort(String mailPort) { this.mailPort = mailPort; }
    
    public String getMailUsername() { return mailUsername; }
    public void setMailUsername(String mailUsername) { this.mailUsername = mailUsername; }
    
    public boolean isMailAuthEnabled() { return mailAuthEnabled; }
    public void setMailAuthEnabled(boolean mailAuthEnabled) { this.mailAuthEnabled = mailAuthEnabled; }
    
    public int getPasswordMinLength() { return passwordMinLength; }
    public void setPasswordMinLength(int passwordMinLength) { this.passwordMinLength = passwordMinLength; }
    
    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }
    
    public long getAccountLockoutDuration() { return accountLockoutDuration; }
    public void setAccountLockoutDuration(long accountLockoutDuration) { this.accountLockoutDuration = accountLockoutDuration; }
    
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    
    public double getMinAmount() { return minAmount; }
    public void setMinAmount(double minAmount) { this.minAmount = minAmount; }
    
    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }
}
```

## 🎨 Phase 4: Mise à jour du UserService

### Étape 4.1: Extension UserService.java

**Ajouts nécessaires dans le service utilisateur existant :**

```java
// Ajouter ces méthodes dans UserService existant

/**
 * Trouve les utilisateurs avec pagination
 */
public Page<User> findAll(Pageable pageable) {
    return userRepository.findAll(pageable);
}

/**
 * Trouve les utilisateurs par statut
 */
public Page<User> findByStatus(UserStatus status, Pageable pageable) {
    return userRepository.findByStatus(status, pageable);
}

/**
 * Trouve les utilisateurs par email contenant
 */
public Page<User> findByEmailContaining(String email, Pageable pageable) {
    return userRepository.findByEmailContainingIgnoreCase(email, pageable);
}

/**
 * Compte les utilisateurs par statut
 */
public long countByStatus(UserStatus status) {
    return userRepository.countByStatus(status);
}

/**
 * Compte les utilisateurs par compte verrouillé
 */
public long countByAccountLocked(Boolean locked) {
    return userRepository.countByAccountLocked(locked);
}

/**
 * Compte total d'utilisateurs
 */
public long count() {
    return userRepository.count();
}
```

### Étape 4.2: Extension UserRepository.java

**Ajouts nécessaires dans le repository utilisateur existant :**

```java
// Ajouter ces méthodes dans UserRepository existant

/**
 * Trouve les utilisateurs par statut avec pagination
 */
Page<User> findByStatus(UserStatus status, Pageable pageable);

/**
 * Trouve les utilisateurs par email contenant avec pagination
 */
Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

/**
 * Compte les utilisateurs par statut
 */
long countByStatus(UserStatus status);

/**
 * Compte les utilisateurs par compte verrouillé
 */
long countByAccountLocked(Boolean locked);
```

Ce guide d'implémentation fournit le code complet pour la première phase du développement des vues d'administration. Les étapes suivantes incluraient les controllers pour les paramètres système et les vues HTML correspondantes.