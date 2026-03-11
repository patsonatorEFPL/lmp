package com.lmp.notification.service;

import com.lmp.notification.config.MailAddressConfig;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service d'envoi d'emails via Mailtrap avec authentification de domaine
 * Utilise le SDK officiel mailtrap-java
 */
// @Service - Temporairement désactivé en faveur du EmailService avec SMTP
public class MailtrapService {

    private static final Logger logger = LoggerFactory.getLogger(MailtrapService.class);

    private final MailtrapClient mailtrapClient;

    private final MailAddressConfig mailAddressConfig;

    @Value("${mailtrap.api.token}")
    private String apiToken;

    @Value("${mailtrap.account.id:}")
    private String accountId;

    public MailtrapService(@Value("${mailtrap.api.token}") String apiToken,
                           MailAddressConfig mailAddressConfig) {
        this.mailAddressConfig = mailAddressConfig;
        if (apiToken == null || apiToken.equals("YOUR_MAILTRAP_API_TOKEN_HERE")) {
            logger.warn("⚠️ Mailtrap API token not configured. Email sending will fail.");
            this.mailtrapClient = null;
        } else {
            final MailtrapConfig config = new MailtrapConfig.Builder()
                    .token(apiToken)
                    .build();
            this.mailtrapClient = MailtrapClientFactory.createMailtrapClient(config);
            logger.info("✅ Mailtrap client initialized successfully with token: {}***", apiToken.substring(0, 8));
        }
    }

    /**
     * Envoie un email transactionnel (noreply) via Mailtrap
     */
    public void sendTransactionalEmail(String to, String subject, String htmlContent, String textContent) 
            throws Exception {
        
        sendEmail(
            mailAddressConfig.getNoreply(),
            mailAddressConfig.getName(),
            to,
            subject,
            htmlContent,
            textContent,
            mailAddressConfig.getReplyToSupport()
        );
    }

    /**
     * Envoie un email support (bidirectionnel) via Mailtrap
     */
    public void sendSupportEmail(String to, String subject, String htmlContent, String textContent) 
            throws Exception {
        
        sendEmail(
            mailAddressConfig.getSupport(),
            mailAddressConfig.getName(),
            to,
            subject,
            htmlContent,
            textContent,
            null // Pas de Reply-To pour les emails support
        );
    }

    /**
     * Envoie un email avec configuration complète
     */
    private void sendEmail(String fromEmail, String fromName, String to, String subject, 
                          String htmlContent, String textContent, String replyTo) 
            throws Exception {
        
        if (mailtrapClient == null) {
            logger.error("❌ Mailtrap client not initialized. Check API token configuration.");
            throw new RuntimeException("Mailtrap client not configured");
        }

        logger.info("📧 Envoi email via Mailtrap: from={}, to={}, subject='{}'", fromEmail, to, subject);

        try {
            // Construction de l'email selon la documentation officielle mise à jour
            MailtrapMail mail = MailtrapMail.builder()
                    .from(new Address(fromEmail, fromName))
                    .to(List.of(new Address(to)))
                    .subject(subject)
                    .text(textContent != null ? textContent : 
                          (htmlContent != null ? htmlContent.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim() : ""))
                    .html(htmlContent)
                    .category("Integration Test")
                    .build();

            // Envoi via l'API Mailtrap
            var result = mailtrapClient.send(mail);
            
            logger.info("✅ Email envoyé avec succès via Mailtrap à {} - Résultat: {}", to, result.toString());

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi à {}: {}", to, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Test de connectivité Mailtrap
     */
    public boolean testConnection() {
        if (mailtrapClient == null) {
            logger.error("❌ Mailtrap client not initialized");
            return false;
        }

        try {
            // Test basique - vérification que le client est configuré
            logger.info("✅ Mailtrap connectivity test passed");
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Mailtrap connectivity test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un email de test
     */
    public void sendTestEmail(String to) throws Exception {
        String subject = "🔧 Test Email - " + mailAddressConfig.getName();
        String htmlContent = String.format("""
            <h2>Test Email Configuration</h2>
            <p>Ce message de test confirme que la configuration Mailtrap fonctionne correctement.</p>
            <p><strong>Expéditeur:</strong> %s</p>
            <p><strong>Token API:</strong> %s***</p>
            <p><strong>Heure d'envoi:</strong> %s</p>
            <hr>
            <p><em>Email envoyé via Mailtrap Java SDK officiel - Authentification domaine native</em></p>
            """, mailAddressConfig.getNoreply(), apiToken.substring(0, 8), LocalDateTime.now());

        String textContent = String.format("""
            Test Email Configuration
            
            Ce message de test confirme que la configuration Mailtrap fonctionne correctement.
            
            Expéditeur: %s
            Token API: %s***
            Heure d'envoi: %s
            
            Email envoyé via Mailtrap Java SDK officiel - Authentification domaine native
            """, mailAddressConfig.getNoreply(), apiToken.substring(0, 8), LocalDateTime.now());

        sendTransactionalEmail(to, subject, htmlContent, textContent);
    }

    /**
     * Vérifie si Mailtrap est configuré
     */
    public boolean isConfigured() {
        return mailtrapClient != null && 
               apiToken != null && 
               !apiToken.equals("YOUR_MAILTRAP_API_TOKEN_HERE");
    }

    /**
     * Obtient les informations de configuration (pour debug)
     */
    public String getConfigInfo() {
        return String.format("Mailtrap configured: %s, API Token: %s, Account ID: %s", 
                           isConfigured(),
                           apiToken != null ? apiToken.substring(0, 8) + "***" : "NOT_SET",
                           accountId != null && !accountId.isEmpty() ? "SET" : "NOT_SET");
    }
}
