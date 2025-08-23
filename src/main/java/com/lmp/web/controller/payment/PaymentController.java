package com.lmp.web.controller.payment;

import com.lmp.service.payment.PaymentService;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.dto.RefundRequestDto;
import com.lmp.service.payment.dto.RefundResponseDto;
import com.lmp.service.payment.exception.PaymentProcessingException;
import com.lmp.service.payment.exception.PaymentValidationException;
import com.lmp.domain.entity.PaymentTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des paiements
 * Fournit les endpoints pour traiter les paiements, remboursements et consultations
 */
@RestController
@RequestMapping("/api/payments")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "https://lmp-digital.ca"})
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + PaymentController.class.getName());
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * Traite un paiement pour une commande
     */
    @PostMapping("/process/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> processPayment(
            @PathVariable @NotNull @Positive Long orderId,
            @Valid @RequestBody PaymentRequestDto paymentRequest) {
        
        logger.info("Processing payment request for order: {}", orderId);
        auditLogger.info("Payment processing initiated - Order: {}, Provider: {}, Amount: {} {}", 
                         orderId, paymentRequest.getPaymentProvider(), 
                         paymentRequest.getAmount(), paymentRequest.getCurrency());
        
        try {
            PaymentResponseDto response = paymentService.processPayment(orderId, paymentRequest);
            
            auditLogger.info("Payment processing completed - Order: {}, Status: {}, Transaction: {}", 
                           orderId, response.getStatus(), response.getTransactionId());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentValidationException e) {
            logger.warn("Payment validation failed for order {}: {}", orderId, e.getMessage());
            auditLogger.warn("Payment validation failed - Order: {}, Error: {}", orderId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "VALIDATION_ERROR");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("field", e.getField());
            errorResponse.put("rejectedValue", e.getRejectedValue());
            
            if (e.hasFieldErrors()) {
                errorResponse.put("fieldErrors", e.getFieldErrors());
            }
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (PaymentProcessingException e) {
            logger.error("Payment processing failed for order {}: {}", orderId, e.getMessage());
            auditLogger.error("Payment processing failed - Order: {}, Error: {}", orderId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "PROCESSING_ERROR");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("errorCode", e.getErrorCode());
            errorResponse.put("provider", e.getPaymentProvider());
            
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error processing payment for order {}: {}", orderId, e.getMessage(), e);
            auditLogger.error("Unexpected payment error - Order: {}, Error: {}", orderId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", "Une erreur inattendue s'est produite");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Effectue un remboursement
     */
    @PostMapping("/refund/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refundPayment(
            @PathVariable @NotNull @Positive Long transactionId,
            @Valid @RequestBody RefundRequestDto refundRequest) {
        
        logger.info("Processing refund request for transaction: {}", transactionId);
        auditLogger.info("Refund processing initiated - Transaction: {}, Amount: {}", 
                         transactionId, refundRequest.getAmount());
        
        try {
            RefundResponseDto response = paymentService.refundPayment(transactionId, refundRequest);
            
            auditLogger.info("Refund processing completed - Transaction: {}, Status: {}, Refund: {}", 
                           transactionId, response.getStatus(), response.getRefundId());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentValidationException e) {
            logger.warn("Refund validation failed for transaction {}: {}", transactionId, e.getMessage());
            auditLogger.warn("Refund validation failed - Transaction: {}, Error: {}", transactionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "VALIDATION_ERROR");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (PaymentProcessingException e) {
            logger.error("Refund processing failed for transaction {}: {}", transactionId, e.getMessage());
            auditLogger.error("Refund processing failed - Transaction: {}, Error: {}", transactionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "PROCESSING_ERROR");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("errorCode", e.getErrorCode());
            
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error processing refund for transaction {}: {}", transactionId, e.getMessage(), e);
            auditLogger.error("Unexpected refund error - Transaction: {}, Error: {}", transactionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", "Une erreur inattendue s'est produite");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Récupère les détails d'une transaction
     */
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTransaction(@PathVariable @NotNull @Positive Long transactionId) {
        
        logger.info("Retrieving transaction details: {}", transactionId);
        
        try {
            PaymentTransaction transaction = paymentService.getTransaction(transactionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", transaction.getId());
            response.put("orderId", transaction.getOrder().getId());
            response.put("amount", transaction.getAmount());
            response.put("currency", transaction.getCurrency());
            response.put("status", transaction.getStatus());
            response.put("paymentMethod", transaction.getPaymentMethod());
            response.put("paymentProvider", transaction.getPaymentProvider());
            response.put("transactionId", transaction.getTransactionId());
            response.put("createdAt", transaction.getCreatedAt());
            response.put("updatedAt", transaction.getUpdatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving transaction {}: {}", transactionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "NOT_FOUND");
            errorResponse.put("message", "Transaction non trouvée");
            
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Récupère toutes les transactions d'une commande
     */
    @GetMapping("/order/{orderId}/transactions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderTransactions(@PathVariable @NotNull @Positive Long orderId) {
        
        logger.info("Retrieving transactions for order: {}", orderId);
        
        try {
            List<PaymentTransaction> transactions = paymentService.getTransactionsByOrder(orderId);
            
            List<Map<String, Object>> response = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> transactionData = new HashMap<>();
                    transactionData.put("id", transaction.getId());
                    transactionData.put("amount", transaction.getAmount());
                    transactionData.put("currency", transaction.getCurrency());
                    transactionData.put("status", transaction.getStatus());
                    transactionData.put("paymentMethod", transaction.getPaymentMethod());
                    transactionData.put("paymentProvider", transaction.getPaymentProvider());
                    transactionData.put("transactionId", transaction.getTransactionId());
                    transactionData.put("createdAt", transaction.getCreatedAt());
                    transactionData.put("updatedAt", transaction.getUpdatedAt());
                    return transactionData;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving transactions for order {}: {}", orderId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "NOT_FOUND");
            errorResponse.put("message", "Commande non trouvée");
            
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Vérifie le statut d'une transaction
     */
    @GetMapping("/transaction/{transactionId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkTransactionStatus(@PathVariable @NotNull @Positive Long transactionId) {
        
        logger.info("Checking status for transaction: {}", transactionId);
        
        try {
            String status = paymentService.checkTransactionStatus(transactionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", transactionId);
            response.put("status", status);
            response.put("checkedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentProcessingException e) {
            logger.error("Error checking transaction status {}: {}", transactionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "STATUS_CHECK_FAILED");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error checking transaction status {}: {}", transactionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "NOT_FOUND");
            errorResponse.put("message", "Transaction non trouvée");
            
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Calcule les frais de traitement Stripe Checkout
     */
    @GetMapping("/fees")
    public ResponseEntity<?> calculateFees(
            @RequestParam @NotNull BigDecimal amount,
            @RequestParam @NotNull String currency,
            @RequestParam(defaultValue = "stripe") String provider) {
        
        logger.info("Calculating fees for amount: {} {}, provider: {}", amount, currency, provider);
        
        try {
            // Forcer le provider à "stripe" car nous n'utilisons que Stripe Checkout
            BigDecimal fees = paymentService.calculateProcessingFees(amount, currency, "stripe");
            
            Map<String, Object> response = new HashMap<>();
            response.put("amount", amount);
            response.put("currency", currency);
            response.put("provider", "stripe");
            response.put("paymentMethod", "checkout_session");
            response.put("fees", fees);
            response.put("totalAmount", amount.add(fees));
            response.put("note", "Frais calculés pour Stripe Checkout uniquement");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error calculating fees: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "CALCULATION_ERROR");
            errorResponse.put("message", "Impossible de calculer les frais");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Récupère les fournisseurs de paiement supportés
     */
    @GetMapping("/providers")
    public ResponseEntity<?> getSupportedProviders() {
        
        try {
            List<String> providers = paymentService.getSupportedProviders();
            
            Map<String, Object> response = new HashMap<>();
            response.put("providers", providers);
            response.put("count", providers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving supported providers: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", "Erreur lors de la récupération des fournisseurs");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Récupère les statistiques de paiement
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentStatistics(@RequestParam(required = false) Long orderId) {
        
        logger.info("Retrieving payment statistics for order: {}", orderId);
        
        try {
            PaymentService.PaymentStatistics stats = paymentService.getPaymentStatistics(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalAmount", stats.getTotalAmount());
            response.put("totalFees", stats.getTotalFees());
            response.put("successfulPayments", stats.getSuccessfulPayments());
            response.put("failedPayments", stats.getFailedPayments());
            response.put("refundedPayments", stats.getRefundedPayments());
            response.put("totalPayments", stats.getTotalPayments());
            response.put("successRate", stats.getSuccessRate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving payment statistics: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", "Erreur lors de la récupération des statistiques");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}