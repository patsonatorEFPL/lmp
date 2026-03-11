package com.lmp.notification.web.debug;

import com.lmp.notification.service.MailtrapServiceHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur de test pour MailtrapServiceHttp
 * 
 * Endpoints pour tester la configuration et l'envoi d'emails via Mailtrap REST API
 * Uniquement disponible en mode développement
 */
@RestController
@RequestMapping("/api/test/mailtrap-http")
public class MailtrapHttpTestController {

    private static final Logger logger = LoggerFactory.getLogger(MailtrapHttpTestController.class);

        private final MailtrapServiceHttp mailtrapService;


    public MailtrapHttpTestController(MailtrapServiceHttp mailtrapService) {
        this.mailtrapService = mailtrapService;
    }

    /**
     * Vérifie la configuration Mailtrap
     * GET /api/test/mailtrap-http/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> checkConfiguration() {
        logger.info("🔍 Vérification de la configuration Mailtrap HTTP");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean configured = mailtrapService.isConfigured();
            String configInfo = mailtrapService.getConfigInfo();
            boolean connectionTest = mailtrapService.testConnection();
            
            response.put("success", true);
            response.put("configured", configured);
            response.put("configInfo", configInfo);
            response.put("connectionTest", connectionTest);
            response.put("message", configured ? 
                "✅ Mailtrap HTTP configuré correctement" : 
                "⚠️ Mailtrap HTTP non configuré - Vérifier API token");
            
            logger.info("📊 Configuration Mailtrap HTTP: configuré={}, connexion={}", configured, connectionTest);
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la vérification de configuration Mailtrap HTTP", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Erreur lors de la vérification");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test de connectivité avec Mailtrap
     * GET /api/test/mailtrap-http/connectivity
     */
    @GetMapping("/connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        logger.info("🔗 Test de connectivité Mailtrap HTTP");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean connectionOk = mailtrapService.testConnection();
            
            response.put("success", true);
            response.put("connected", connectionOk);
            response.put("message", connectionOk ? 
                "✅ Connexion Mailtrap HTTP réussie" : 
                "❌ Échec de connexion à Mailtrap HTTP");
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors du test de connectivité Mailtrap HTTP", e);
            response.put("success", false);
            response.put("connected", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Erreur lors du test de connexion");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Envoi d'un email de test transactionnel
     * POST /api/test/mailtrap-http/send-test
     * 
     * Body JSON: {"email": "destinataire@example.com"}
     */
    @PostMapping("/send-test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "❌ Email destinataire requis");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        logger.info("📧 Envoi d'email de test Mailtrap HTTP vers: {}", email);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            mailtrapService.sendTestEmail(email);
            
            response.put("success", true);
            response.put("recipient", email);
            response.put("message", "✅ Email de test envoyé avec succès via Mailtrap REST API!");
            response.put("instructions", "Vérifiez votre boîte email et l'en-tête 'From' pour confirmer l'authentification du domaine");
            
            logger.info("✅ Email de test Mailtrap HTTP envoyé avec succès à {}", email);
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de test à {}: {}", email, e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Erreur lors de l'envoi");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Envoi d'un email support de test
     * POST /api/test/mailtrap-http/send-support
     * 
     * Body JSON: {"email": "destinataire@example.com", "subject": "Test Subject", "message": "Test message"}
     */
    @PostMapping("/send-support")
    public ResponseEntity<Map<String, Object>> sendSupportTestEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String subject = request.getOrDefault("subject", "Test Email Support - LMP Services");
        String message = request.getOrDefault("message", "Ceci est un message de test du service support.");
        
        if (email == null || email.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "❌ Email destinataire requis");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        logger.info("📧 Envoi d'email support de test Mailtrap HTTP vers: {}", email);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String htmlContent = String.format("""
                <h2>%s</h2>
                <p>%s</p>
                <p><strong>Envoyé depuis:</strong> support@lmp-services.ca</p>
                <p><strong>Heure:</strong> %s</p>
                <hr>
                <p><em>Email envoyé via Mailtrap REST API pour test</em></p>
                """, subject, message, java.time.LocalDateTime.now());
            
            mailtrapService.sendSupportEmail(email, subject, htmlContent, message);
            
            response.put("success", true);
            response.put("recipient", email);
            response.put("subject", subject);
            response.put("message", "✅ Email support de test envoyé avec succès via Mailtrap REST API!");
            
            logger.info("✅ Email support de test Mailtrap HTTP envoyé avec succès à {}", email);
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi d'email support de test à {}: {}", email, e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Erreur lors de l'envoi");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}
