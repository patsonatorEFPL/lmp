package com.lmp.service.payment;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.domain.enums.PaymentStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.PaymentTransactionRepository;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.dto.RefundRequestDto;
import com.lmp.service.payment.dto.RefundResponseDto;
import com.lmp.service.payment.dto.WebhookEventDto;
import com.lmp.service.payment.exception.PaymentProcessingException;
import com.lmp.service.payment.exception.PaymentValidationException;
import com.lmp.service.payment.webhook.StripeWebhookHandler;
import com.lmp.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation du service de paiement principal
 * Orchestre les différents processeurs de paiement et gère la persistance
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + PaymentServiceImpl.class.getName());
    
    @Autowired
    private Map<String, PaymentProcessor> paymentProcessors;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private StripeWebhookHandler stripeWebhookHandler;
    
    @Override
    public PaymentResponseDto processPayment(java.util.UUID orderId, PaymentRequestDto paymentRequest)
            throws PaymentProcessingException, PaymentValidationException {
        
        logger.info("Processing payment for order {} with provider {}", orderId, paymentRequest.getPaymentProvider());
        logger.debug("Payment request details - Order: {}, Amount: {} {}, Currency: {}, Method: {}",
                    orderId, paymentRequest.getAmount(), paymentRequest.getCurrency(), paymentRequest.getPaymentMethod());
        
        // Récupérer la commande
        logger.debug("Retrieving order from database: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec ID: " + orderId));
        
        logger.debug("Order retrieved - ID: {}, Service: '{}', Amount: {}, Status: {}",
                    order.getId(), order.getServiceName(), order.getTotalAmount(), order.getStatus());
                
        // Vérifier que la commande peut être payée
        validateOrderForPayment(order);
        
        // Valider les données de paiement
        validatePaymentRequest(paymentRequest, paymentRequest.getPaymentProvider());
        
        // Récupérer le processeur de paiement approprié
        PaymentProcessor processor = getPaymentProcessor(paymentRequest.getPaymentProvider());
        
        logger.debug("Creating pending transaction for order: {}", orderId);
        // Créer une transaction en attente
        PaymentTransaction transaction = createPendingTransaction(order, paymentRequest);
        logger.debug("Pending transaction created with ID: {}", transaction.getId());
        
        try {
            logger.debug("Starting payment processing with processor for order: {}", orderId);
            // Traiter le paiement
            PaymentResponseDto response = processor.processPayment(order, paymentRequest);
            
            logger.debug("Payment processor response received for order: {} - Status: {}", orderId, response.getStatus());
            
            // Mettre à jour la transaction avec la réponse
            updateTransactionFromResponse(transaction, response);
            
            // Sauvegarder la transaction
            logger.debug("Saving transaction to database: {}", transaction.getId());
            paymentTransactionRepository.save(transaction);
            logger.debug("Transaction saved successfully: {}", transaction.getId());
            
            // Mettre à jour le statut de la commande si le paiement est réussi
            if (response.isSuccessful()) {
                logger.debug("Payment successful, updating order status for order: {}", orderId);
                updateOrderStatus(order, OrderStatus.IN_PROGRESS);
                logger.debug("Order status updated to IN_PROGRESS for order: {}", orderId);
                auditLogger.info("Payment successful - Order: {}, Transaction: {}, Amount: {}",
                               orderId, transaction.getId(), response.getAmount(), response.getCurrency());
            } else if (response.isFailed()) {
                logger.warn("Payment failed for order: {} - Error: {}", orderId, response.getErrorMessage());
                auditLogger.warn("Payment failed - Order: {}, Transaction: {}, Error: {}",
                                orderId, transaction.getId(), response.getErrorMessage());
            }
            
            // Ajouter l'ID de transaction interne à la réponse
            response.setTransactionId(String.valueOf(transaction.getId()));
            
            logger.info("Payment processing completed for order {} - Status: {}", orderId, response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error during payment processing for order {}: {}", orderId, e.getMessage(), e);
            // Marquer la transaction comme échouée
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            logger.debug("Updating transaction status to FAILED: {}", transaction.getId());
            paymentTransactionRepository.save(transaction);
            
            auditLogger.error("Payment processing failed - Order: {}, Transaction: {}, Error: {}",
                             orderId, transaction.getId(), e.getMessage());
            
            throw e;
        }
    }
    
    @Override
    public RefundResponseDto refundPayment(java.util.UUID transactionId, RefundRequestDto refundRequest)
            throws PaymentProcessingException, PaymentValidationException {
        
        logger.info("Processing refund for transaction {} with amount {}", transactionId, refundRequest.getAmount());
        
        // Récupérer la transaction
        PaymentTransaction transaction = getTransaction(transactionId);
        
        // Valider que la transaction peut être remboursée
        validateTransactionForRefund(transaction, refundRequest);
        
        // Récupérer le processeur de paiement approprié
        PaymentProcessor processor = getPaymentProcessor(transaction.getPaymentProvider());
        
        try {
            // Traiter le remboursement
            RefundResponseDto response = processor.refundPayment(transaction, refundRequest);
            
            // Créer une nouvelle transaction de remboursement
            PaymentTransaction refundTransaction = createRefundTransaction(transaction, refundRequest, response);
            paymentTransactionRepository.save(refundTransaction);
            
            // Mettre à jour la transaction originale si remboursement complet
            if (!refundRequest.isPartialRefund()) {
                transaction.setStatus(PaymentStatus.REFUNDED);
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentTransactionRepository.save(transaction);
            }
            
            auditLogger.info("Refund processed - Original Transaction: {}, Refund Transaction: {}, Amount: {} {}", 
                           transactionId, refundTransaction.getId(), refundRequest.getAmount(), transaction.getCurrency());
            
            logger.info("Refund processing completed for transaction {} - Status: {}", transactionId, response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            auditLogger.error("Refund processing failed - Transaction: {}, Error: {}", transactionId, e.getMessage());
            throw e;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public String checkTransactionStatus(java.util.UUID transactionId) throws PaymentProcessingException {
        PaymentTransaction transaction = getTransaction(transactionId);
        PaymentProcessor processor = getPaymentProcessor(transaction.getPaymentProvider());
        
        try {
            String providerStatus = processor.checkTransactionStatus(transaction.getTransactionId());
            
            // Mettre à jour le statut local si nécessaire
            PaymentStatus localStatus = mapProviderStatusToLocal(providerStatus, transaction.getPaymentProvider());
            if (localStatus != transaction.getStatus()) {
                transaction.setStatus(localStatus);
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentTransactionRepository.save(transaction);
                
                logger.info("Transaction status updated - Transaction: {}, New Status: {}", 
                           transactionId, localStatus);
            }
            
            return providerStatus;
            
        } catch (Exception e) {
            logger.error("Error checking transaction status - Transaction: {}, Error: {}", 
                        transactionId, e.getMessage());
            throw e;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentTransaction getTransaction(java.util.UUID transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée avec ID: " + transactionId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getTransactionsByOrder(java.util.UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec ID: " + orderId));
        
        return paymentTransactionRepository.findByOrder(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateProcessingFees(BigDecimal amount, String currency, String provider) {
        PaymentProcessor processor = getPaymentProcessor(provider);
        return processor.calculateProcessingFees(amount, currency);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isProviderAvailable(String provider) {
        try {
            PaymentProcessor processor = getPaymentProcessor(provider);
            return processor.isAvailable();
        } catch (Exception e) {
            logger.warn("Provider {} is not available: {}", provider, e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getSupportedProviders() {
        return paymentProcessors.values().stream()
                .map(PaymentProcessor::getProcessorName)
                .collect(Collectors.toList());
    }
    
    @Override
    public WebhookEventDto processWebhook(String provider, String payload, String signature) 
            throws PaymentProcessingException {
        
        logger.info("Processing webhook from provider: {}", provider);
        
        switch (provider.toLowerCase()) {
            case "stripe":
                return stripeWebhookHandler.processWebhook(payload, signature);
            default:
                throw new PaymentProcessingException(
                    "Fournisseur de webhook non supporté: " + provider,
                    "UNSUPPORTED_PROVIDER",
                    provider
                );
        }
    }
    
    @Override
    public void validatePaymentRequest(PaymentRequestDto paymentRequest, String provider) 
            throws PaymentValidationException {
        
        // Validation générale
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Le montant doit être supérieur à 0");
        }
        
        if (paymentRequest.getCurrency() == null || paymentRequest.getCurrency().trim().isEmpty()) {
            throw new PaymentValidationException("La devise est requise");
        }
        
        // Validation spécifique au fournisseur
        PaymentProcessor processor = getPaymentProcessor(provider);
        processor.validatePayment(paymentRequest);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentStatistics getPaymentStatistics(java.util.UUID orderId) {
        List<PaymentTransaction> transactions;
        
        if (orderId != null) {
            transactions = getTransactionsByOrder(orderId);
        } else {
            transactions = paymentTransactionRepository.findAll();
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        int successful = 0;
        int failed = 0;
        int refunded = 0;
        
        for (PaymentTransaction transaction : transactions) {
            if (transaction.getAmount() != null) {
                totalAmount = totalAmount.add(transaction.getAmount());
            }
            
            switch (transaction.getStatus()) {
                case COMPLETED:
                    successful++;
                    break;
                case FAILED:
                    failed++;
                    break;
                case REFUNDED:
                    refunded++;
                    break;
            }
        }
        
        return new PaymentStatistics(totalAmount, totalFees, successful, failed, refunded);
    }
    
    /**
     * Récupère le processeur de paiement approprié
     */
    private PaymentProcessor getPaymentProcessor(String provider) {
        String processorBeanName = provider.toLowerCase() + "PaymentProcessor";
        
        // Debug logging pour diagnostiquer le problème
        logger.error("DEBUG - Recherche du processeur: provider='{}', beanName='{}'", provider, processorBeanName);
        logger.error("DEBUG - Beans disponibles: {}", paymentProcessors.keySet());
        
        PaymentProcessor processor = paymentProcessors.get(processorBeanName);
        
        if (processor == null) {
            logger.error("DEBUG - Processeur non trouvé pour le provider: {}", provider);
            throw new IllegalArgumentException("Fournisseur de paiement non supporté: " + provider);
        }
        
        return processor;
    }
    
    /**
     * Valide qu'une commande peut être payée
     */
    private void validateOrderForPayment(Order order) throws PaymentValidationException {
        logger.debug("Validating order for payment - ID: {}, Status: {}, Amount: {}",
                    order.getId(), order.getStatus(), order.getTotalAmount());
                    
        // 🆕 Accepter les commandes PAYMENT_PENDING et PENDING
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            logger.warn("Order {} cannot be paid in current state: {}", order.getId(), order.getStatus());
            throw new PaymentValidationException("La commande ne peut pas être payée dans son état actuel: " + order.getStatus());
        }
        
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Order {} has invalid amount for payment: {}", order.getId(), order.getTotalAmount());
            throw new PaymentValidationException("Le montant de la commande doit être supérieur à 0");
        }
        
        logger.debug("Order {} validation passed", order.getId());
    }
    
    /**
     * Crée une transaction en attente
     */
    private PaymentTransaction createPendingTransaction(Order order, PaymentRequestDto paymentRequest) {
        logger.debug("Creating pending transaction for order: {} - Amount: {} {}, Provider: {}",
                    order.getId(), paymentRequest.getAmount(), paymentRequest.getCurrency(), paymentRequest.getPaymentProvider());
                    
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setAmount(paymentRequest.getAmount());
        transaction.setCurrency(paymentRequest.getCurrency());
        transaction.setPaymentMethod(paymentRequest.getPaymentMethod());
        transaction.setPaymentProvider(paymentRequest.getPaymentProvider());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        
        logger.debug("Saving pending transaction to database for order: {}", order.getId());
        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
        logger.debug("Pending transaction saved successfully - ID: {}, Order: {}", savedTransaction.getId(), order.getId());
        
        return savedTransaction;
    }
    
    /**
     * Met à jour une transaction avec la réponse du processeur
     */
    private void updateTransactionFromResponse(PaymentTransaction transaction, PaymentResponseDto response) {
        transaction.setTransactionId(response.getProviderTransactionId());
        transaction.setStatus(response.getStatus());
        transaction.setUpdatedAt(LocalDateTime.now());
    }
    
    /**
     * Met à jour le statut d'une commande
     */
    private void updateOrderStatus(Order order, OrderStatus status) {
        logger.debug("Updating order {} status from {} to {}", order.getId(), order.getStatus(), status);
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        logger.debug("Saving order {} to database with new status: {}", order.getId(), status);
        Order savedOrder = orderRepository.save(order);
        logger.debug("Order {} saved successfully with new status: {}", savedOrder.getId(), savedOrder.getStatus());
    }
    
    /**
     * Valide qu'une transaction peut être remboursée
     */
    private void validateTransactionForRefund(PaymentTransaction transaction, RefundRequestDto refundRequest) 
            throws PaymentValidationException {
        
        if (transaction.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentValidationException("Seules les transactions complétées peuvent être remboursées");
        }
        
        if (refundRequest.getAmount().compareTo(transaction.getAmount()) > 0) {
            throw new PaymentValidationException("Le montant du remboursement ne peut pas dépasser le montant de la transaction");
        }
    }
    
    /**
     * Crée une transaction de remboursement
     */
    private PaymentTransaction createRefundTransaction(PaymentTransaction originalTransaction, 
                                                     RefundRequestDto refundRequest, 
                                                     RefundResponseDto response) {
        PaymentTransaction refundTransaction = new PaymentTransaction();
        refundTransaction.setOrder(originalTransaction.getOrder());
        refundTransaction.setAmount(refundRequest.getAmount().negate()); // Montant négatif pour le remboursement
        refundTransaction.setCurrency(originalTransaction.getCurrency());
        refundTransaction.setPaymentMethod(originalTransaction.getPaymentMethod());
        refundTransaction.setPaymentProvider(originalTransaction.getPaymentProvider());
        refundTransaction.setTransactionId(response.getProviderRefundId());
        refundTransaction.setStatus(response.getStatus());
        refundTransaction.setCreatedAt(LocalDateTime.now());
        refundTransaction.setUpdatedAt(LocalDateTime.now());
        
        return refundTransaction;
    }
    
    /**
     * Mappe le statut du fournisseur vers le statut local
     */
    private PaymentStatus mapProviderStatusToLocal(String providerStatus, String provider) {
        if ("stripe".equals(provider.toLowerCase())) {
            return switch (providerStatus.toLowerCase()) {
                case "succeeded" -> PaymentStatus.COMPLETED;
                case "failed", "canceled" -> PaymentStatus.FAILED;
                case "requires_action", "requires_confirmation", "processing" -> PaymentStatus.PENDING;
                default -> PaymentStatus.PENDING;
            };
        }
        
        // Mapping par défaut
        return PaymentStatus.PENDING;
    }
}