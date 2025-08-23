package com.lmp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class ConfigDebugController {

    @Autowired
    private Environment env;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfigInfo() {
        Map<String, Object> configInfo = new HashMap<>();
        
        // Profils actifs
        configInfo.put("activeProfiles", env.getActiveProfiles());
        configInfo.put("defaultProfiles", env.getDefaultProfiles());
        
        // Sources de configuration détectées
        configInfo.put("springProfilesActive", env.getProperty("spring.profiles.active"));
        configInfo.put("javaOpts", System.getProperty("java.opts"));
        configInfo.put("springConfigLocation", env.getProperty("spring.config.location"));
        
        // Configuration base de données
        configInfo.put("datasourceUrl", env.getProperty("spring.datasource.url"));
        configInfo.put("datasourceUsername", env.getProperty("spring.datasource.username"));
        
        // Configuration Stripe
        configInfo.put("stripeSecretKey", maskSecret(env.getProperty("stripe.secret.key")));
        configInfo.put("stripePublishableKey", env.getProperty("stripe.publishable.key"));
        
        // Configuration email
        configInfo.put("mailHost", env.getProperty("spring.mail.host"));
        configInfo.put("mailUsername", env.getProperty("spring.mail.username"));
        
        // Variables d'environnement importantes
        configInfo.put("PORT", System.getenv("PORT"));
        configInfo.put("DATABASE_URL", maskSecret(System.getenv("DATABASE_URL")));
        configInfo.put("SPRING_PROFILES_ACTIVE", System.getenv("SPRING_PROFILES_ACTIVE"));
        
        return ResponseEntity.ok(configInfo);
    }
    
    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 10) {
            return secret;
        }
        return secret.substring(0, 6) + "***" + secret.substring(secret.length() - 4);
    }
}