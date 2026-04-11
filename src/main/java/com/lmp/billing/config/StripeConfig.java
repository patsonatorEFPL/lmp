package com.lmp.billing.config;

import com.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * Configuration Stripe pour l'application LMP.
 * Expose un {@link StripeClient} centralisé (recommandé v32+).
 * <p>
 * Toutes les opérations Stripe passent par le {@link StripeClient} injecté.
 * La vérification de signature webhook utilise {@code Webhook.constructEvent}
 * qui est une méthode utilitaire statique indépendante du client.
 */
@Configuration
public class StripeConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeConfig.class);
    
    private final Environment environment;
    
    @Value("${stripe.secret.key:}")
    private String stripeSecretKey;
    
    @Value("${stripe.publishable.key:}")
    private String stripePublishableKey;
    
    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${stripe.api.version:2026-03-25.dahlia}")
    private String apiVersion;
    
    @Value("${stripe.connect.client.id:}")
    private String connectClientId;
    
    @Value("${stripe.max.network.retries:3}")
    private int maxNetworkRetries;
    
    @Value("${stripe.connect.timeout:30000}")
    private int connectTimeout;
    
    @Value("${stripe.read.timeout:80000}")
    private int readTimeout;

    @Value("${company.website:http://localhost:8080}")
    private String companyWebsite;

    public StripeConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Vérifie la configuration au démarrage.
     */
    @PostConstruct
    public void initStripe() {
        boolean isDevProfile = java.util.Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (isDevProfile && (stripeSecretKey == null || stripeSecretKey.trim().isEmpty())) {
            logger.warn("Stripe keys not configured — Stripe is DISABLED in dev mode. "
                    + "Set stripe.secret.key and stripe.publishable.key in application-secrets.properties to enable.");
            return;
        }

        validateStripeKeys();
        logger.info("Stripe configuration initialized successfully - API Version: {}", apiVersion);
    }

    /**
     * Bean principal {@link StripeClient} — à injecter dans tous les services Stripe.
     * <p>
     * Remplace l'ancien pattern {@code Stripe.apiKey = ...} + appels statiques.
     */
    @Bean
    public StripeClient stripeClient() {
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty()) {
            logger.warn("Stripe secret key not configured — returning placeholder StripeClient. "
                    + "Any Stripe API call will fail at runtime. Configure stripe.secret.key to enable.");
            return new StripeClient.StripeClientBuilder()
                    .setApiKey("sk_placeholder_not_configured")
                    .build();
        }

        // Note : la version d'API est définie par le SDK (v32 → 2026-03-25.dahlia).
        // La propriété stripe.api.version est utilisée pour le logging et les métadonnées.
        return new StripeClient.StripeClientBuilder()
                .setApiKey(stripeSecretKey)
                .setMaxNetworkRetries(maxNetworkRetries)
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    /**
     * Bean pour les propriétés Stripe (pour injection dans d'autres composants).
     */
    @Bean
    public StripeProperties stripeProperties() {
        StripeProperties properties = new StripeProperties();
        properties.setSecretKey(stripeSecretKey != null ? stripeSecretKey : "");
        properties.setPublishableKey(stripePublishableKey != null ? stripePublishableKey : "");
        properties.setWebhookSecret(webhookSecret != null ? webhookSecret : "");
        properties.setApiVersion(apiVersion);
        properties.setConnectClientId(connectClientId);
        properties.setMaxNetworkRetries(maxNetworkRetries);
        properties.setConnectTimeout(connectTimeout);
        properties.setReadTimeout(readTimeout);
        return properties;
    }
    
    /**
     * Configuration spécifique pour l'environnement de développement.
     */
    @Configuration
    @Profile("dev")
    static class StripeDevConfig {
        @PostConstruct
        public void initDevConfig() {
            logger.warn("Stripe running in DEVELOPMENT mode - Use test keys only!");
        }
    }
    
    /**
     * Configuration spécifique pour l'environnement de production.
     */
    @Configuration
    @Profile("prod")
    static class StripeProdConfig {
        @PostConstruct
        public void initProdConfig() {
            logger.info("Stripe running in PRODUCTION mode");
        }
    }
    
    /**
     * Valide que toutes les clés Stripe requises sont présentes.
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
        
        if (!stripeSecretKey.startsWith("sk_")) {
            throw new IllegalStateException("Invalid Stripe secret key format");
        }
        
        if (!stripePublishableKey.startsWith("pk_")) {
            throw new IllegalStateException("Invalid Stripe publishable key format");
        }
        
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
     * Classe pour encapsuler les propriétés Stripe.
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
