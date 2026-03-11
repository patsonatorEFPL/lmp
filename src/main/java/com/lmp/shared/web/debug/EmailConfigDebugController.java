package com.lmp.shared.web.debug;

import com.lmp.notification.config.MailAddressConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller de débogage pour vérifier la configuration email
 * À supprimer après résolution du problème
 */
@RestController
@RequestMapping("/debug/email-config")
public class EmailConfigDebugController {

        private final MailAddressConfig mailAddressConfig;
    
        private final Environment environment;
    
    @Value("${mail.from.address:NOT_SET}")
    private String oldFromAddress;
    
    @Value("${company.email:NOT_SET}")
    private String companyEmail;


    public EmailConfigDebugController(MailAddressConfig mailAddressConfig,
                           Environment environment) {
        this.mailAddressConfig = mailAddressConfig;
        this.environment = environment;
    }

    @GetMapping
    public Map<String, Object> getEmailConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Configuration du MailAddressConfig
        config.put("mailAddressConfig", Map.of(
            "noreply", mailAddressConfig.getNoreply(),
            "support", mailAddressConfig.getSupport(),
            "replyToSupport", mailAddressConfig.getReplyToSupport(),
            "name", mailAddressConfig.getName()
        ));
        
        // Anciennes propriétés
        config.put("legacyConfig", Map.of(
            "mail.from.address", oldFromAddress,
            "company.email", companyEmail
        ));
        
        // Configuration Environment directe
        config.put("environmentConfig", Map.of(
            "mail.from.noreply", environment.getProperty("mail.from.noreply", "NOT_SET"),
            "mail.from.support", environment.getProperty("mail.from.support", "NOT_SET"),
            "mail.replyto.support", environment.getProperty("mail.replyto.support", "NOT_SET"),
            "company.email", environment.getProperty("company.email", "NOT_SET")
        ));
        
        // Profile actif
        config.put("activeProfiles", environment.getActiveProfiles());
        
        return config;
    }
    
    @GetMapping("/test-strategy")
    public Map<String, Object> testEmailStrategy() {
        Map<String, Object> strategy = new HashMap<>();
        
        // Test de la stratégie d'adresses
        strategy.put("transactionalEmail", Map.of(
            "from", mailAddressConfig.getAppropriateFromAddress(true),
            "replyTo", mailAddressConfig.getAppropriateReplyTo(true)
        ));
        
        strategy.put("supportEmail", Map.of(
            "from", mailAddressConfig.getAppropriateFromAddress(false),
            "replyTo", mailAddressConfig.getAppropriateReplyTo(false)
        ));
        
        return strategy;
    }
}
