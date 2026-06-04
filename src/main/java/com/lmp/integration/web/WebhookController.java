package com.lmp.integration.web;

import com.lmp.billing.service.PaymentService;
import com.lmp.billing.dto.WebhookEventDto;
import com.lmp.billing.exception.PaymentProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des webhooks de paiement
 * Traite les notifications asynchrones des fournisseurs de paiement
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY." + WebhookController.class.getName());
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + WebhookController.class.getName());
    
        private final PaymentService paymentService;
    

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint pour les webhooks Stripe
     */
    @PostMapping("/stripe")
    public ResponseEntity<?> handleStripeWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {
        
        logger.info("DEBUG WEBHOOK CONTROLLER - Received Stripe webhook");
        logger.info("DEBUG WEBHOOK CONTROLLER - Payload length: {}", payload != null ? payload.length() : 0);
        logger.info("DEBUG WEBHOOK CONTROLLER - Request headers: User-Agent={}, Content-Type={}",
                   request.getHeader("User-Agent"), request.getHeader("Content-Type"));
        
        try {
            // Récupérer la signature Stripe
            String signature = request.getHeader("Stripe-Signature");
            
            if (signature == null || signature.trim().isEmpty()) {
                securityLogger.warn("Stripe webhook received without signature");
                logger.error("DEBUG WEBHOOK CONTROLLER - Missing signature header");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("MISSING_SIGNATURE", "Signature manquante"));
            }
            
            logger.info("DEBUG WEBHOOK CONTROLLER - Signature present, processing webhook");
            
            // Traiter le webhook
            WebhookEventDto webhookEvent = paymentService.processWebhook("stripe", payload, signature);
            
            logger.info("DEBUG WEBHOOK CONTROLLER - Webhook processed successfully: {}", webhookEvent.getEventType());
            
            // Logger les détails de sécurité
            securityLogger.info("Stripe webhook processed - Event: {}, Type: {}, Status: {}", 
                               webhookEvent.getEventId(), webhookEvent.getEventType(), 
                               webhookEvent.isProcessed() ? "SUCCESS" : "FAILED");
            
            auditLogger.info("Webhook processed - Provider: stripe, Event: {}, Type: {}, Transaction: {}", 
                           webhookEvent.getEventId(), webhookEvent.getEventType(), 
                           webhookEvent.getProviderTransactionId());
            
            // Créer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("received", true);
            response.put("eventId", webhookEvent.getEventId());
            response.put("eventType", webhookEvent.getEventType());
            response.put("processed", webhookEvent.isProcessed());
            response.put("processedAt", webhookEvent.getProcessedAt());
            
            if (webhookEvent.hasError()) {
                response.put("error", webhookEvent.getProcessingError());
                logger.warn("Webhook processing completed with error - Event: {}, Error: {}", 
                           webhookEvent.getEventId(), webhookEvent.getProcessingError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentProcessingException e) {
            logger.error("DEBUG WEBHOOK CONTROLLER - Stripe webhook processing failed: {}", e.getMessage());
            securityLogger.error("Stripe webhook processing failed - Error: {}, Code: {}",
                                e.getMessage(), e.getErrorCode());
            
            if ("INVALID_SIGNATURE".equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("INVALID_SIGNATURE", "Signature invalide"));
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("PROCESSING_ERROR", e.getMessage()));
            
        } catch (Exception e) {
            logger.error("DEBUG WEBHOOK CONTROLLER - Unexpected error processing Stripe webhook: {}", e.getMessage(), e);
            securityLogger.error("Unexpected Stripe webhook error: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erreur interne du serveur"));
        }
    }
    
    /**
     * Endpoint pour les webhooks PayPal (futur)
     */
    @PostMapping("/paypal")
    public ResponseEntity<?> handlePayPalWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {
        
        logger.info("Received PayPal webhook");
        
        try {
            // Récupérer les en-têtes PayPal
            String authAlgo = request.getHeader("PAYPAL-AUTH-ALGO");
            String transmission = request.getHeader("PAYPAL-TRANSMISSION-ID");
            String certId = request.getHeader("PAYPAL-CERT-ID");
            String signature = request.getHeader("PAYPAL-TRANSMISSION-SIG");
            String timestamp = request.getHeader("PAYPAL-TRANSMISSION-TIME");
            
            securityLogger.info("PayPal webhook received - Transmission: {}, Cert: {}", transmission, certId);
            
            // TODO: Implémenter le traitement PayPal
            WebhookEventDto webhookEvent = paymentService.processWebhook("paypal", payload, signature);
            
            auditLogger.info("PayPal webhook processed - Event: {}, Type: {}", 
                           webhookEvent.getEventId(), webhookEvent.getEventType());
            
            Map<String, Object> response = new HashMap<>();
            response.put("received", true);
            response.put("eventId", webhookEvent.getEventId());
            response.put("processed", webhookEvent.isProcessed());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentProcessingException e) {
            logger.error("PayPal webhook processing failed: {}", e.getMessage());
            securityLogger.error("PayPal webhook processing failed: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("PROCESSING_ERROR", e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Unexpected error processing PayPal webhook: {}", e.getMessage(), e);
            securityLogger.error("Unexpected PayPal webhook error: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erreur interne du serveur"));
        }
    }
    
    /**
     * Endpoint pour les webhooks Square (futur)
     */
    @PostMapping("/square")
    public ResponseEntity<?> handleSquareWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {
        
        logger.info("Received Square webhook");
        
        try {
            // Récupérer la signature Square
            String signature = request.getHeader("X-Square-Signature");
            
            if (signature == null || signature.trim().isEmpty()) {
                securityLogger.warn("Square webhook received without signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("MISSING_SIGNATURE", "Signature manquante"));
            }
            
            securityLogger.info("Square webhook received with signature");
            
            // TODO: Implémenter le traitement Square
            WebhookEventDto webhookEvent = paymentService.processWebhook("square", payload, signature);
            
            auditLogger.info("Square webhook processed - Event: {}, Type: {}", 
                           webhookEvent.getEventId(), webhookEvent.getEventType());
            
            Map<String, Object> response = new HashMap<>();
            response.put("received", true);
            response.put("eventId", webhookEvent.getEventId());
            response.put("processed", webhookEvent.isProcessed());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentProcessingException e) {
            logger.error("Square webhook processing failed: {}", e.getMessage());
            securityLogger.error("Square webhook processing failed: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("PROCESSING_ERROR", e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Unexpected error processing Square webhook: {}", e.getMessage(), e);
            securityLogger.error("Unexpected Square webhook error: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erreur interne du serveur"));
        }
    }
    
    /**
     * Endpoint générique pour tester la connectivité des webhooks
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("supportedProviders", java.util.List.of("stripe", "paypal", "square"));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint pour récupérer les logs de webhooks (admin seulement)
     */
    @GetMapping("/logs")
    @ResponseBody
    public ResponseEntity<?> getWebhookLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String eventType) {
        
        logger.info("Retrieving webhook logs - Page: {}, Size: {}, Provider: {}, EventType: {}", 
                   page, size, provider, eventType);
        
        try {
            // TODO: Implémenter la récupération des logs depuis une base de données
            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", 0);
            response.put("totalPages", 0);
            response.put("content", java.util.List.of());
            response.put("message", "Fonctionnalité en cours de développement");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving webhook logs: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erreur lors de la récupération des logs"));
        }
    }
    
    /**
     * Crée une réponse d'erreur standardisée
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        return errorResponse;
    }
    
    /**
     * Lit le contenu du body de la requête
     */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        String line;
        
        try (var reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }
        
        return buffer.toString();
    }
    
    /**
     * Valide l'adresse IP source pour la sécurité (optionnel)
     */
    private boolean isValidSourceIP(String clientIP, String provider) {
        // TODO: Implémenter la validation des IPs selon le fournisseur
        // Stripe: https://stripe.com/docs/ips
        // PayPal: https://developer.paypal.com/docs/api-basics/notifications/webhooks/notification-messages/
        
        logger.debug("Validating source IP: {} for provider: {}", clientIP, provider);
        return true; // Pour l'instant, accepter toutes les IPs
    }
}