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

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des webhooks de paiement.
 * Traite les notifications asynchrones des fournisseurs de paiement.
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

        logger.debug("Stripe webhook received - payload length: {}", payload != null ? payload.length() : 0);

        try {
            String signature = request.getHeader("Stripe-Signature");

            if (signature == null || signature.trim().isEmpty()) {
                securityLogger.warn("Stripe webhook received without signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("MISSING_SIGNATURE", "Signature manquante"));
            }

            WebhookEventDto webhookEvent = paymentService.processWebhook("stripe", payload, signature);

            securityLogger.info("Stripe webhook processed - Event: {}, Type: {}, Status: {}",
                               webhookEvent.getEventId(), webhookEvent.getEventType(),
                               webhookEvent.isProcessed() ? "SUCCESS" : "FAILED");

            auditLogger.info("Webhook processed - Provider: stripe, Event: {}, Type: {}, Transaction: {}",
                           webhookEvent.getEventId(), webhookEvent.getEventType(),
                           webhookEvent.getProviderTransactionId());

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
            securityLogger.error("Stripe webhook processing failed - Error: {}, Code: {}",
                                e.getMessage(), e.getErrorCode());

            if ("INVALID_SIGNATURE".equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("INVALID_SIGNATURE", "Signature invalide"));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("PROCESSING_ERROR", e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error processing Stripe webhook: {}", e.getMessage(), e);
            securityLogger.error("Unexpected Stripe webhook error: {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erreur interne du serveur"));
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
}
