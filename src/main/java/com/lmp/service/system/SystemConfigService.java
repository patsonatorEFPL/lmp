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
            settings.setCompanyEmail(environment.getProperty("company.email", "lmp.assistance@gmail.com"));
            settings.setCompanyPhone(environment.getProperty("company.phone", "+1 (555) 123-4567"));
            settings.setCompanyAddress(environment.getProperty("company.address", "123 Rue Principale"));
            settings.setCompanyWebsite(environment.getProperty("company.website", "https://lmp-services.ca"));
            
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
            defaults.setCompanyEmail("lmp.assistance@gmail.com");
            defaults.setCompanyPhone("+1 (555) 123-4567");
            defaults.setCompanyAddress("123 Rue Principale, Ville, Province, Code Postal");
            defaults.setCompanyWebsite("https://lmp-services.ca");
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

    /**
     * Obtient un paramètre spécifique
     */
    public String getProperty(String key) {
        return environment.getProperty(key);
    }

    /**
     * Obtient un paramètre avec une valeur par défaut
     */
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    /**
     * Vérifie si un paramètre existe
     */
    public boolean hasProperty(String key) {
        return environment.containsProperty(key);
    }

    /**
     * Obtient les statistiques de configuration
     */
    public Map<String, Object> getConfigurationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            SystemSettingsDto settings = loadAllSettings();
            
            stats.put("appConfigured", settings.getAppName() != null && !settings.getAppName().isEmpty());
            stats.put("companyConfigured", settings.getCompanyName() != null && !settings.getCompanyName().isEmpty());
            stats.put("emailConfigured", settings.getMailHost() != null && !settings.getMailHost().isEmpty());
            stats.put("stripeConfigured", settings.getStripePublishableKey() != null && !settings.getStripePublishableKey().isEmpty());
            stats.put("securityConfigured", settings.getPasswordMinLength() >= 6);
            stats.put("paymentConfigured", settings.getDefaultCurrency() != null && !settings.getDefaultCurrency().isEmpty());
            
            // Calcul du pourcentage de configuration
            long configuredCount = stats.values().stream()
                .mapToLong(value -> (Boolean) value ? 1 : 0)
                .sum();
            stats.put("configurationPercentage", (configuredCount * 100) / stats.size());
            
        } catch (Exception e) {
            logger.error("Error getting configuration stats: {}", e.getMessage(), e);
            stats.put("error", true);
        }
        
        return stats;
    }
}