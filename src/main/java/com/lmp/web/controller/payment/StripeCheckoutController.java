package com.lmp.web.controller.payment;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.PaymentTransactionRepository;
import com.lmp.service.payment.PaymentService;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.processor.StripeCheckoutPaymentProcessor;
import com.lmp.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur pour les paiements Stripe Checkout
 * Gère les sessions de checkout et les URLs de callback
 */
@Controller
@RequestMapping("/stripe/checkout")
public class StripeCheckoutController {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeCheckoutController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + StripeCheckoutController.class.getName());
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private StripeCheckoutPaymentProcessor stripeCheckoutProcessor;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    /**
     * Crée une session Stripe Checkout pour une commande
     */
    @PostMapping("/create-session/{orderId}")
    @ResponseBody
    public ResponseEntity<?> createCheckoutSession(@PathVariable Long orderId,
                                                  HttpServletRequest request) {
        
        logger.info("Creating Stripe Checkout session for order: {}", orderId);
        auditLogger.info("Stripe Checkout session creation initiated - Order: {}, IP: {}", 
                         orderId, getClientIpAddress(request));
        
        try {
            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));
            
            // Vérifier que la commande peut être payée (PAYMENT_PENDING ou PENDING)
            if (order.getStatus() != OrderStatus.PAYMENT_PENDING && order.getStatus() != OrderStatus.PENDING) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_ORDER_STATUS",
                    "message", "Cette commande ne peut pas être payée dans son état actuel"
                ));
            }
            
            // Créer la requête de paiement
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setAmount(order.getTotalAmount());
            paymentRequest.setCurrency("CAD");
            paymentRequest.setPaymentProvider("stripe");
            paymentRequest.setPaymentMethod("checkout_session");
            
            // Ajouter les métadonnées
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("order_id", orderId);
            metadata.put("customer_ip", getClientIpAddress(request));
            metadata.put("user_agent", request.getHeader("User-Agent"));
            paymentRequest.setMetadata(metadata);
            
            // Traiter le paiement (créer la session)
            PaymentResponseDto response = paymentService.processPayment(orderId, paymentRequest);
            
            // Debug logging pour diagnostiquer le problème
            logger.error("DEBUG - Response: successful={}, requiresRedirect={}, redirectUrl={}, errorMessage={}",
                        response.isSuccessful(), response.isRequiresRedirect(),
                        response.getRedirectUrl(), response.getErrorMessage());
            
            if ((response.isSuccessful() || response.isPending()) && response.isRequiresRedirect()) {
                auditLogger.info("Stripe Checkout session created successfully - Order: {}, Session: {}",
                               orderId, response.getProviderTransactionId());
                
                // 🆕 Sauvegarder le stripeSessionId dans l'order
                order.setStripeSessionId(response.getProviderTransactionId());
                orderRepository.save(order);
                
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("redirectUrl", response.getRedirectUrl());
                successResponse.put("sessionId", response.getProviderTransactionId());
                
                return ResponseEntity.ok(successResponse);
                
            } else {
                logger.warn("Failed to create Stripe Checkout session for order: {}", orderId);
                auditLogger.warn("Stripe Checkout session creation failed - Order: {}, Error: {}", 
                                orderId, response.getErrorMessage());
                
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                    "error", "SESSION_CREATION_FAILED",
                    "message", response.getErrorMessage() != null ? 
                               response.getErrorMessage() : "Impossible de créer la session de paiement"
                ));
            }
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found for Stripe Checkout: {}", orderId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Unexpected error creating Stripe Checkout session for order {}: {}", 
                        orderId, e.getMessage(), e);
            auditLogger.error("Stripe Checkout session creation error - Order: {}, Error: {}", 
                             orderId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Une erreur inattendue s'est produite"
            ));
        }
    }
    
    /**
     * Page de succès après paiement Stripe Checkout
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("order_id") Long orderId,
                                @RequestParam("session_id") String sessionId,
                                @RequestParam(defaultValue = "success") String type,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        
        logger.info("Processing Stripe Checkout success callback - Order: {}, Session: {}", orderId, sessionId);
        auditLogger.info("Payment success callback - Order: {}, Session: {}, Type: {}", 
                         orderId, sessionId, type);
        
        try {
            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));
            
            // Récupérer la transaction
            Optional<PaymentTransaction> transactionOpt = 
                paymentTransactionRepository.findByTransactionId(sessionId);
            
            if (transactionOpt.isPresent()) {
                PaymentTransaction transaction = transactionOpt.get();
                
                // Ajouter les informations au modèle
                model.addAttribute("order", order);
                model.addAttribute("transaction", transaction);
                model.addAttribute("sessionId", sessionId);
                model.addAttribute("success", true);
                
                // Calculer les détails de paiement
                model.addAttribute("totalAmount", order.getTotalAmount());
                model.addAttribute("currency", "CAD");
                model.addAttribute("paymentMethod", "Stripe Checkout");
                
                auditLogger.info("Payment success page displayed - Order: {}, Amount: {} CAD", 
                               orderId, order.getTotalAmount());
                
                return "payment/success";
                
            } else {
                logger.warn("Transaction not found for successful payment - Session: {}", sessionId);
                redirectAttributes.addFlashAttribute("error", 
                    "Transaction non trouvée. Veuillez contacter le support.");
                return "redirect:/services";
            }
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found in success callback: {}", orderId);
            redirectAttributes.addFlashAttribute("error", 
                "Commande non trouvée. Veuillez contacter le support.");
            return "redirect:/services";
            
        } catch (Exception e) {
            logger.error("Error processing payment success callback - Order: {}, Session: {}: {}", 
                        orderId, sessionId, e.getMessage(), e);
            auditLogger.error("Payment success callback error - Order: {}, Session: {}, Error: {}", 
                             orderId, sessionId, e.getMessage());
            
            redirectAttributes.addFlashAttribute("error", 
                "Une erreur s'est produite. Veuillez contacter le support.");
            return "redirect:/services";
        }
    }
    
    /**
     * Page d'annulation après annulation Stripe Checkout
     */
    @GetMapping("/cancel")
    public String paymentCancel(@RequestParam(value = "order_id", required = false) Long orderId,
                               @RequestParam(value = "session_id", required = false) String sessionId,
                               @RequestParam(defaultValue = "cancel") String type,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        logger.info("Processing Stripe Checkout cancel callback - Order: {}, Session: {}", orderId, sessionId);
        auditLogger.info("Payment cancel callback - Order: {}, Session: {}, Type: {}",
                         orderId, sessionId, type);
        
        try {
            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));
            
            // Récupérer la page source depuis les métadonnées de la session Stripe
            String sourcePage = getSourcePageFromStripeSession(sessionId);
            
            auditLogger.info("Payment cancel callback - Order: {}, Amount: {} CAD, Source: {}",
                           orderId, order.getTotalAmount(), sourcePage);
            
            redirectAttributes.addFlashAttribute("info",
                "Paiement annulé. Vous pouvez réessayer à tout moment.");
            
            // Rediriger vers la page source ou vers services par défaut
            return "redirect:" + (sourcePage != null ? sourcePage : "/services");
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found in cancel callback: {}", orderId);
            redirectAttributes.addFlashAttribute("info",
                "Paiement annulé. Vous pouvez réessayer à tout moment.");
            return "redirect:/services";
            
        } catch (Exception e) {
            logger.error("Error processing payment cancel callback - Order: {}, Session: {}: {}",
                        orderId, sessionId, e.getMessage(), e);
            auditLogger.error("Payment cancel callback error - Order: {}, Session: {}, Error: {}",
                             orderId, sessionId, e.getMessage());
            
            redirectAttributes.addFlashAttribute("info",
                "Paiement annulé. Vous pouvez réessayer à tout moment.");
            return "redirect:/services";
        }
    }
    
    /**
     * API pour vérifier le statut d'une session de paiement
     */
    @GetMapping("/status/{sessionId}")
    @ResponseBody
    public ResponseEntity<?> getPaymentStatus(@PathVariable String sessionId) {
        
        logger.info("Checking payment status for session: {}", sessionId);
        
        try {
            // Vérifier le statut via le service
            String status = paymentService.checkTransactionStatus(
                paymentTransactionRepository.findByTransactionId(sessionId)
                    .map(PaymentTransaction::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"))
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("status", status);
            response.put("checkedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Error checking payment status for session {}: {}", sessionId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "STATUS_CHECK_FAILED",
                "message", "Impossible de vérifier le statut du paiement"
            ));
        }
    }
    
    /**
     * Récupère la page source depuis les métadonnées de la session Stripe
     */
    private String getSourcePageFromStripeSession(String sessionId) {
        try {
            // Configurer la clé API Stripe
            com.stripe.Stripe.apiKey = stripeSecretKey;
            
            // Récupérer la session depuis Stripe
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.retrieve(sessionId);
            
            if (session.getMetadata() != null && session.getMetadata().containsKey("source_page")) {
                String sourcePage = session.getMetadata().get("source_page");
                logger.info("Retrieved source page from Stripe session {}: {}", sessionId, sourcePage);
                return sourcePage;
            }
            
            logger.info("No source_page metadata found in Stripe session: {}", sessionId);
            
        } catch (Exception e) {
            logger.warn("Unable to retrieve source page from Stripe session {}: {}", sessionId, e.getMessage());
        }
        
        return null; // Retourner null si pas trouvé
    }
    
    /**
     * Utilitaire pour récupérer l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}