package com.lmp.config;

import com.stripe.Stripe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Configuration Stripe pour l'application LMP
 * Gère l'initialisation sécurisée de l'API Stripe
 */
@Configuration
public class StripeConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeConfig.class);
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @Value("${stripe.api.version:2023-10-16}")
    private String apiVersion;
    
    @Value("${stripe.connect.client.id:}")
    private String connectClientId;
    
    @Value("${stripe.max.network.retries:3}")
    private int maxNetworkRetries;
    
    @Value("${stripe.connect.timeout:30000}")
    private int connectTimeout;
    
    @Value("${stripe.read.timeout:80000}")
    private int readTimeout;
    
    /**
     * Initialise la configuration Stripe globale
     */
    @PostConstruct
    public void initStripe() {
        try {
            // Validation des clés requises
            validateStripeKeys();
            
            // Configuration globale de Stripe
            Stripe.apiKey = stripeSecretKey;
            Stripe.setMaxNetworkRetries(maxNetworkRetries);
            Stripe.setConnectTimeout(connectTimeout);
            Stripe.setReadTimeout(readTimeout);
            
            // Configuration de l'agent utilisateur
            Stripe.setAppInfo(
                "LMP-Payment-System",
                "1.0.0",
                "https://lmp-services.ca",
                "pp_partner_LMP"
            );
            
            logger.info("Stripe configuration initialized successfully - API Version: {}", apiVersion);
            
        } catch (Exception e) {
            logger.error("Failed to initialize Stripe configuration: {}", e.getMessage(), e);
            throw new IllegalStateException("Stripe configuration failed", e);
        }
    }
    
    /**
     * Bean pour les propriétés Stripe (pour injection dans d'autres composants)
     */
    @Bean
    public StripeProperties stripeProperties() {
        StripeProperties properties = new StripeProperties();
        properties.setSecretKey(stripeSecretKey);
        properties.setPublishableKey(stripePublishableKey);
        properties.setWebhookSecret(webhookSecret);
        properties.setApiVersion(apiVersion);
        properties.setConnectClientId(connectClientId);
        properties.setMaxNetworkRetries(maxNetworkRetries);
        properties.setConnectTimeout(connectTimeout);
        properties.setReadTimeout(readTimeout);
        return properties;
    }
    
    /**
     * Configuration spécifique pour l'environnement de développement
     */
    @Configuration
    @Profile("dev")
    static class StripeDevConfig {
        
        @PostConstruct
        public void initDevConfig() {
            logger.warn("Stripe running in DEVELOPMENT mode - Use test keys only!");
            
            // Configuration additionnelle pour le développement
            Stripe.enableTelemetry = false; // Désactiver la télémétrie en dev
        }
    }
    
    /**
     * Configuration spécifique pour l'environnement de production
     */
    @Configuration
    @Profile("prod")
    static class StripeProdConfig {
        
        @PostConstruct
        public void initProdConfig() {
            logger.info("Stripe running in PRODUCTION mode");
            
            // Configuration additionnelle pour la production
            Stripe.enableTelemetry = true; // Activer la télémétrie en prod
        }
    }
    
    /**
     * Valide que toutes les clés Stripe requises sont présentes
     */
    private void validateStripeKeys() {
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe secret key is required");
        }
        
        if (stripePublishableKey == null || stripePublishableKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe publishable key is required");
        }
        
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            logger.warn("Stripe webhook secret is not configured - Webhook signature verification will be disabled");
        }
        
        // Validation du format des clés
        if (!stripeSecretKey.startsWith("sk_")) {
            throw new IllegalStateException("Invalid Stripe secret key format");
        }
        
        if (!stripePublishableKey.startsWith("pk_")) {
            throw new IllegalStateException("Invalid Stripe publishable key format");
        }
        
        // Vérification de l'environnement des clés
        boolean isTestKey = stripeSecretKey.contains("_test_");
        boolean isProdKey = stripeSecretKey.contains("_live_");
        
        if (!isTestKey && !isProdKey) {
            logger.warn("Unable to determine Stripe key environment");
        } else if (isTestKey) {
            logger.info("Using Stripe TEST keys");
        } else {
            logger.info("Using Stripe LIVE keys");
        }
    }
    
    /**
     * Classe pour encapsuler les propriétés Stripe
     */
    public static class StripeProperties {
        private String secretKey;
        private String publishableKey;
        private String webhookSecret;
        private String apiVersion;
        private String connectClientId;
        private int maxNetworkRetries;
        private int connectTimeout;
        private int readTimeout;
        
        // Getters et Setters
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        
        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }
        
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
        
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        
        public String getConnectClientId() { return connectClientId; }
        public void setConnectClientId(String connectClientId) { this.connectClientId = connectClientId; }
        
        public int getMaxNetworkRetries() { return maxNetworkRetries; }
        public void setMaxNetworkRetries(int maxNetworkRetries) { this.maxNetworkRetries = maxNetworkRetries; }
        
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
        
        public boolean isTestMode() {
            return secretKey != null && secretKey.contains("_test_");
        }
        
        public boolean isLiveMode() {
            return secretKey != null && secretKey.contains("_live_");
        }
    }
}