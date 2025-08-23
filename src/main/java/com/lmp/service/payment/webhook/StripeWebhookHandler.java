package com.lmp.service.payment.webhook;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.domain.enums.PaymentStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.PaymentTransactionRepository;
import com.lmp.service.payment.dto.WebhookEventDto;
import com.lmp.service.payment.exception.PaymentProcessingException;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestionnaire des webhooks Stripe
 * Traite les événements asynchrones envoyés par Stripe
 * Assure la sécurité avec la vérification des signatures
 */
@Service
public class StripeWebhookHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookHandler.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY." + StripeWebhookHandler.class.getName());
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    /**
     * Traite un webhook Stripe avec vérification de signature
     */
    @Transactional
    public WebhookEventDto processWebhook(String payload, String sigHeader) throws PaymentProcessingException {
        logger.info("🔍 DEBUG WEBHOOK - Processing Stripe webhook event");
        logger.info("🔍 DEBUG WEBHOOK - Payload length: {}, Signature present: {}",
                   payload != null ? payload.length() : 0, sigHeader != null);
        
        try {
            // Vérifier la signature du webhook pour la sécurité
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            
            securityLogger.info("Webhook signature verified - Event: {}, Type: {}", 
                               event.getId(), event.getType());
            
            // Créer le DTO d'événement
            WebhookEventDto webhookEvent = new WebhookEventDto(
                event.getId(),
                event.getType(),
                "stripe"
            );
            webhookEvent.setEventTime(LocalDateTime.ofEpochSecond(event.getCreated(), 0, ZoneOffset.UTC));
            webhookEvent.setRawPayload(payload);
            webhookEvent.setSignature(sigHeader);
            
            // Traiter l'événement selon son type
            try {
                processEventByType(event, webhookEvent);
                webhookEvent.setProcessed(true);
                
                logger.info("Webhook event processed successfully - Event: {}, Type: {}", 
                           event.getId(), event.getType());
                
            } catch (Exception e) {
                logger.error("Error processing webhook event {}: {}", event.getId(), e.getMessage(), e);
                webhookEvent.setProcessed(false);
                webhookEvent.setProcessingError(e.getMessage());
                
                securityLogger.warn("Webhook processing failed - Event: {}, Error: {}", 
                                   event.getId(), e.getMessage());
            }
            
            return webhookEvent;
            
        } catch (SignatureVerificationException e) {
            securityLogger.error("Webhook signature verification failed: {}", e.getMessage());
            throw new PaymentProcessingException(
                "Signature de webhook invalide",
                "INVALID_SIGNATURE",
                "stripe"
            );
        } catch (Exception e) {
            logger.error("Unexpected error processing webhook: {}", e.getMessage(), e);
            securityLogger.error("Unexpected webhook processing error: {}", e.getMessage());
            throw new PaymentProcessingException(
                "Erreur lors du traitement du webhook",
                "WEBHOOK_ERROR",
                "stripe"
            );
        }
    }
    
    /**
     * Traite les événements selon leur type
     */
    private void processEventByType(Event event, WebhookEventDto webhookEvent) {
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event, webhookEvent);
                break;
                
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event, webhookEvent);
                break;
                
            case "payment_intent.requires_action":
                handlePaymentIntentRequiresAction(event, webhookEvent);
                break;
                
            case "charge.dispute.created":
                handleChargeDisputeCreated(event, webhookEvent);
                break;
                
            case "refund.created":
                handleRefundCreated(event, webhookEvent);
                break;
                
            case "refund.updated":
                handleRefundUpdated(event, webhookEvent);
                break;
                
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event, webhookEvent);
                break;
                
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event, webhookEvent);
                break;
                
            case "checkout.session.expired":
                handleCheckoutSessionExpired(event, webhookEvent);
                break;
                
            case "checkout.session.async_payment_succeeded":
                handleCheckoutSessionAsyncPaymentSucceeded(event, webhookEvent);
                break;
                
            case "checkout.session.async_payment_failed":
                handleCheckoutSessionAsyncPaymentFailed(event, webhookEvent);
                break;
                
            case "customer.subscription.updated":
                handleSubscriptionUpdated(event, webhookEvent);
                break;
                
            default:
                logger.info("Unhandled webhook event type: {}", event.getType());
                webhookEvent.setStatus("unhandled");
        }
    }
    
    /**
     * Gère les paiements réussis
     */
    private void handlePaymentIntentSucceeded(Event event, WebhookEventDto webhookEvent) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (paymentIntent != null) {
            logger.info("🔍 DEBUG WEBHOOK - handlePaymentIntentSucceeded called for PaymentIntent: {}", paymentIntent.getId());
            logger.info("🔍 DEBUG WEBHOOK - PaymentIntent status: {}, amount: {}, currency: {}",
                       paymentIntent.getStatus(), paymentIntent.getAmount(), paymentIntent.getCurrency());
            logger.info("🔍 DEBUG WEBHOOK - PaymentIntent metadata: {}", paymentIntent.getMetadata());
            
            webhookEvent.setProviderTransactionId(paymentIntent.getId());
            webhookEvent.setStatus("succeeded");
            
            // Mettre à jour la transaction dans la base de données
            updatePaymentTransactionStatus(paymentIntent.getId(), PaymentStatus.COMPLETED);
            
            // 🆕 NOUVEAUTÉ CRUCIALE : Mettre à jour aussi le statut de l'Order
            updateOrderStatusByPaymentIntentId(paymentIntent.getId(), OrderStatus.CONFIRMED, paymentIntent);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("amount", paymentIntent.getAmount());
            eventData.put("currency", paymentIntent.getCurrency());
            eventData.put("payment_method", paymentIntent.getPaymentMethod());
            eventData.put("metadata", paymentIntent.getMetadata());
            webhookEvent.setEventData(eventData);
            
            logger.info("✅ Payment succeeded - PaymentIntent: {}, Amount: {} {}",
                       paymentIntent.getId(), paymentIntent.getAmount(), paymentIntent.getCurrency());
        } else {
            logger.warn("❌ PaymentIntent is null in handlePaymentIntentSucceeded");
        }
    }
    
    /**
     * Gère les échecs de paiement
     */
    private void handlePaymentIntentFailed(Event event, WebhookEventDto webhookEvent) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (paymentIntent != null) {
            webhookEvent.setProviderTransactionId(paymentIntent.getId());
            webhookEvent.setStatus("failed");
            
            // Mettre à jour la transaction dans la base de données
            updatePaymentTransactionStatus(paymentIntent.getId(), PaymentStatus.FAILED);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("amount", paymentIntent.getAmount());
            eventData.put("currency", paymentIntent.getCurrency());
            eventData.put("last_payment_error", paymentIntent.getLastPaymentError());
            webhookEvent.setEventData(eventData);
            
            logger.warn("Payment failed - PaymentIntent: {}, Error: {}", 
                       paymentIntent.getId(), paymentIntent.getLastPaymentError());
            
            securityLogger.info("Payment failure recorded - PaymentIntent: {}", paymentIntent.getId());
        }
    }
    
    /**
     * Gère les paiements nécessitant une action supplémentaire
     */
    private void handlePaymentIntentRequiresAction(Event event, WebhookEventDto webhookEvent) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (paymentIntent != null) {
            webhookEvent.setProviderTransactionId(paymentIntent.getId());
            webhookEvent.setStatus("requires_action");
            
            // Mettre à jour la transaction dans la base de données
            updatePaymentTransactionStatus(paymentIntent.getId(), PaymentStatus.PENDING);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("amount", paymentIntent.getAmount());
            eventData.put("currency", paymentIntent.getCurrency());
            eventData.put("next_action", paymentIntent.getNextAction());
            webhookEvent.setEventData(eventData);
            
            logger.info("Payment requires action - PaymentIntent: {}", paymentIntent.getId());
        }
    }
    
    /**
     * Gère la création de litiges
     */
    private void handleChargeDisputeCreated(Event event, WebhookEventDto webhookEvent) {
        // Traitement des litiges - notification aux administrateurs
        webhookEvent.setStatus("dispute_created");
        
        logger.warn("Charge dispute created - Event: {}", event.getId());
        securityLogger.warn("Payment dispute created - Event: {}", event.getId());
        
        // TODO: Implémenter la notification aux administrateurs
    }
    
    /**
     * Gère la création de remboursements
     */
    private void handleRefundCreated(Event event, WebhookEventDto webhookEvent) {
        Refund refund = (Refund) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (refund != null) {
            webhookEvent.setProviderTransactionId(refund.getPaymentIntent());
            webhookEvent.setStatus("refund_created");
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("refund_id", refund.getId());
            eventData.put("amount", refund.getAmount());
            eventData.put("currency", refund.getCurrency());
            eventData.put("reason", refund.getReason());
            webhookEvent.setEventData(eventData);
            
            logger.info("Refund created - Refund: {}, Amount: {} {}", 
                       refund.getId(), refund.getAmount(), refund.getCurrency());
        }
    }
    
    /**
     * Gère la mise à jour de remboursements
     */
    private void handleRefundUpdated(Event event, WebhookEventDto webhookEvent) {
        Refund refund = (Refund) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (refund != null) {
            webhookEvent.setProviderTransactionId(refund.getPaymentIntent());
            webhookEvent.setStatus("refund_updated");
            
            // Mettre à jour le statut si le remboursement est réussi
            if ("succeeded".equals(refund.getStatus())) {
                updatePaymentTransactionStatus(refund.getPaymentIntent(), PaymentStatus.REFUNDED);
            }
            
            logger.info("Refund updated - Refund: {}, Status: {}", refund.getId(), refund.getStatus());
        }
    }
    
    /**
     * Gère les paiements de facture réussis
     */
    private void handleInvoicePaymentSucceeded(Event event, WebhookEventDto webhookEvent) {
        webhookEvent.setStatus("invoice_paid");
        logger.info("Invoice payment succeeded - Event: {}", event.getId());
    }
    
    /**
     * Gère la completion des sessions Checkout
     */
    private void handleCheckoutSessionCompleted(Event event, WebhookEventDto webhookEvent) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("completed");
            
            // Mettre à jour la transaction dans la base de données
            updatePaymentTransactionStatus(session.getId(), PaymentStatus.COMPLETED);
            
            // 🆕 NOUVEAUTÉ : Mettre à jour le statut de l'Order via stripeSessionId
            logger.info("🔍 DEBUG WEBHOOK - Checkout completed, updating order status for session: {}", session.getId());
            updateOrderStatusByStripeSessionId(session.getId(), OrderStatus.CONFIRMED);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", session.getId());
            eventData.put("payment_intent", session.getPaymentIntent());
            eventData.put("customer_email", session.getCustomerEmail());
            eventData.put("amount_total", session.getAmountTotal());
            eventData.put("currency", session.getCurrency());
            eventData.put("payment_status", session.getPaymentStatus());
            webhookEvent.setEventData(eventData);
            
            logger.info("Checkout session completed - Session: {}, PaymentIntent: {}, Amount: {} {}",
                       session.getId(), session.getPaymentIntent(), session.getAmountTotal(), session.getCurrency());
            
            securityLogger.info("Checkout session completed - Session: {}, Customer: {}",
                               session.getId(), session.getCustomerEmail());
        }
    }
    
    /**
     * Gère l'expiration des sessions Checkout
     */
    private void handleCheckoutSessionExpired(Event event, WebhookEventDto webhookEvent) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("expired");
            
            // Marquer la transaction comme échouée ou expirée
            updatePaymentTransactionStatus(session.getId(), PaymentStatus.FAILED);
            
            // 🆕 NOUVEAUTÉ : Mettre à jour le statut de l'Order pour indiquer l'échec
            updateOrderStatusByStripeSessionId(session.getId(), OrderStatus.CANCELLED);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", session.getId());
            eventData.put("customer_email", session.getCustomerEmail());
            eventData.put("expires_at", session.getExpiresAt());
            webhookEvent.setEventData(eventData);
            
            logger.warn("Checkout session expired - Session: {}, Customer: {}",
                       session.getId(), session.getCustomerEmail());
            
            securityLogger.info("Checkout session expired - Session: {}", session.getId());
        }
    }
    
    /**
     * Gère les paiements asynchrones réussis (ex: virements bancaires)
     */
    private void handleCheckoutSessionAsyncPaymentSucceeded(Event event, WebhookEventDto webhookEvent) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("async_payment_succeeded");
            
            // Mettre à jour la transaction comme complétée
            updatePaymentTransactionStatus(session.getId(), PaymentStatus.COMPLETED);
            
            // 🆕 NOUVEAUTÉ : Mettre à jour le statut de l'Order pour indiquer le succès
            logger.info("🔍 DEBUG WEBHOOK - Async payment succeeded, updating order status for session: {}", session.getId());
            updateOrderStatusByStripeSessionId(session.getId(), OrderStatus.CONFIRMED);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", session.getId());
            eventData.put("payment_intent", session.getPaymentIntent());
            eventData.put("customer_email", session.getCustomerEmail());
            eventData.put("payment_status", session.getPaymentStatus());
            webhookEvent.setEventData(eventData);
            
            logger.info("Checkout session async payment succeeded - Session: {}, PaymentIntent: {}",
                       session.getId(), session.getPaymentIntent());
            
            securityLogger.info("Checkout async payment succeeded - Session: {}, Customer: {}",
                               session.getId(), session.getCustomerEmail());
        }
    }
    
    /**
     * Gère les échecs de paiements asynchrones
     */
    private void handleCheckoutSessionAsyncPaymentFailed(Event event, WebhookEventDto webhookEvent) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("async_payment_failed");
            
            // Marquer la transaction comme échouée
            updatePaymentTransactionStatus(session.getId(), PaymentStatus.FAILED);
            
            // 🆕 NOUVEAUTÉ : Mettre à jour le statut de l'Order pour indiquer l'échec
            updateOrderStatusByStripeSessionId(session.getId(), OrderStatus.CANCELLED);
            
            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", session.getId());
            eventData.put("customer_email", session.getCustomerEmail());
            eventData.put("payment_status", session.getPaymentStatus());
            webhookEvent.setEventData(eventData);
            
            logger.warn("Checkout session async payment failed - Session: {}, Customer: {}",
                       session.getId(), session.getCustomerEmail());
            
            securityLogger.warn("Checkout async payment failed - Session: {}", session.getId());
        }
    }
    
    /**
     * Gère les mises à jour d'abonnement
     */
    private void handleSubscriptionUpdated(Event event, WebhookEventDto webhookEvent) {
        webhookEvent.setStatus("subscription_updated");
        logger.info("Subscription updated - Event: {}", event.getId());
    }
    
    /**
     * Met à jour le statut d'une transaction de paiement
     */
    private void updatePaymentTransactionStatus(String providerTransactionId, PaymentStatus status) {
        try {
            Optional<PaymentTransaction> transactionOpt = 
                paymentTransactionRepository.findByTransactionId(providerTransactionId);
            
            if (transactionOpt.isPresent()) {
                PaymentTransaction transaction = transactionOpt.get();
                transaction.setStatus(status);
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentTransactionRepository.save(transaction);
                
                logger.info("Payment transaction status updated - Transaction: {}, Status: {}", 
                           providerTransactionId, status);
            } else {
                logger.warn("Payment transaction not found for update - Transaction: {}", 
                           providerTransactionId);
            }
        } catch (Exception e) {
            logger.error("Error updating payment transaction status - Transaction: {}, Error: {}", 
                        providerTransactionId, e.getMessage(), e);
        }
    }
    
    /**
     * 🆕 NOUVEAUTÉ : Met à jour le statut d'une commande par son stripeSessionId
     * Cette méthode est cruciale pour résoudre le problème de persistance prématurée des commandes
     */
    private void updateOrderStatusByStripeSessionId(String stripeSessionId, OrderStatus newStatus) {
        try {
            logger.info("🔍 DEBUG WEBHOOK - Searching for order with stripeSessionId: {}", stripeSessionId);
            Optional<Order> orderOpt = orderRepository.findByStripeSessionId(stripeSessionId);
            
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                OrderStatus oldStatus = order.getStatus();
                
                logger.info("🔍 DEBUG WEBHOOK - Found order: {}, current status: {}, requested status: {}",
                           order.getId(), oldStatus, newStatus);
                
                // Mettre à jour le statut seulement si c'est un changement logique
                if (shouldUpdateOrderStatus(oldStatus, newStatus)) {
                    order.setStatus(newStatus);
                    order.setUpdatedAt(LocalDateTime.now());
                    
                    // Ajouter des informations de paiement si disponible
                    if (newStatus == OrderStatus.CONFIRMED) {
                        order.setPaidAt(LocalDateTime.now());
                        order.setPaymentStatus("succeeded");
                    }
                    
                    orderRepository.save(order);
                    
                    logger.info("✅ Order status updated via webhook - Order: {}, Stripe Session: {}, Status: {} -> {}",
                               order.getId(), stripeSessionId, oldStatus, newStatus);
                    
                    securityLogger.info("Order status updated via Stripe webhook - Order: {}, Session: {}",
                                       order.getId(), stripeSessionId);
                } else {
                    logger.warn("❌ Order status update skipped - Order: {}, Current: {}, Requested: {}, Reason: Invalid transition",
                                order.getId(), oldStatus, newStatus);
                }
            } else {
                logger.error("❌ Order not found for Stripe session update - Session: {}", stripeSessionId);
                
                // Log toutes les commandes existantes pour debug
                List<Order> allOrders = orderRepository.findAll();
                logger.info("🔍 DEBUG WEBHOOK - Total orders in database: {}", allOrders.size());
                for (Order o : allOrders) {
                    logger.info("🔍 DEBUG WEBHOOK - Order {}: stripeSessionId={}, status={}",
                               o.getId(), o.getStripeSessionId(), o.getStatus());
                }
            }
        } catch (Exception e) {
            logger.error("❌ Error updating order status via webhook - Session: {}, Error: {}",
                        stripeSessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 🆕 NOUVEAUTÉ : Met à jour le statut d'une commande par son PaymentIntent ID
     * Cette méthode recherche la commande par PaymentIntent et met à jour son statut
     */
    private void updateOrderStatusByPaymentIntentId(String paymentIntentId, OrderStatus newStatus, PaymentIntent paymentIntent) {
        try {
            logger.info("🔍 DEBUG WEBHOOK - Searching for order with PaymentIntent ID: {}", paymentIntentId);
            Optional<Order> orderOpt = orderRepository.findByStripePaymentIntentId(paymentIntentId);
            
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                OrderStatus oldStatus = order.getStatus();
                
                logger.info("🔍 DEBUG WEBHOOK - Found order: {}, current status: {}, requested status: {}",
                           order.getId(), oldStatus, newStatus);
                
                // Mettre à jour le statut seulement si c'est un changement logique
                if (shouldUpdateOrderStatus(oldStatus, newStatus)) {
                    order.setStatus(newStatus);
                    order.setUpdatedAt(LocalDateTime.now());
                    
                    // Ajouter des informations de paiement si disponible
                    if (newStatus == OrderStatus.CONFIRMED) {
                        order.setPaidAt(LocalDateTime.now());
                        order.setPaymentStatus("succeeded");
                    }
                    
                    orderRepository.save(order);
                    
                    logger.info("✅ Order status updated via PaymentIntent webhook - Order: {}, PaymentIntent: {}, Status: {} -> {}",
                               order.getId(), paymentIntentId, oldStatus, newStatus);
                    
                    securityLogger.info("Order status updated via Stripe PaymentIntent webhook - Order: {}, PaymentIntent: {}",
                                       order.getId(), paymentIntentId);
                } else {
                    logger.warn("❌ Order status update skipped - Order: {}, Current: {}, Requested: {}, Reason: Invalid transition",
                                order.getId(), oldStatus, newStatus);
                }
            } else {
                logger.warn("❌ Order not found for PaymentIntent ID: {}", paymentIntentId);
                
                // Tentative de recherche par metadata session_id
                if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("session_id")) {
                    String sessionId = paymentIntent.getMetadata().get("session_id");
                    logger.info("🔍 DEBUG WEBHOOK - Trying alternative search by session_id from metadata: {}", sessionId);
                    updateOrderStatusByStripeSessionId(sessionId, newStatus);
                } else {
                    logger.error("❌ No session_id in PaymentIntent metadata, cannot find order - PaymentIntent: {}", paymentIntentId);
                    
                    // Log toutes les commandes existantes pour debug
                    List<Order> allOrders = orderRepository.findAll();
                    logger.info("🔍 DEBUG WEBHOOK - Total orders in database: {}", allOrders.size());
                    for (Order o : allOrders) {
                        logger.info("🔍 DEBUG WEBHOOK - Order {}: stripePaymentIntentId={}, stripeSessionId={}, status={}",
                                   o.getId(), o.getStripePaymentIntentId(), o.getStripeSessionId(), o.getStatus());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("❌ Error updating order status via PaymentIntent webhook - PaymentIntent: {}, Error: {}",
                        paymentIntentId, e.getMessage(), e);
        }
    }
    
    /**
     * 🆕 NOUVEAUTÉ : Détermine si une mise à jour de statut d'Order est logique
     * Évite les mises à jour inappropriées et maintient la cohérence des données
     */
    private boolean shouldUpdateOrderStatus(OrderStatus currentStatus, OrderStatus newStatus) {
        logger.info("🔍 DEBUG WEBHOOK - Checking status transition: {} -> {}", currentStatus, newStatus);
        
        // Permettre les transitions depuis PAYMENT_PENDING
        if (currentStatus == OrderStatus.PAYMENT_PENDING) {
            boolean allowed = newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.PENDING || newStatus == OrderStatus.CANCELLED;
            logger.info("🔍 DEBUG WEBHOOK - From PAYMENT_PENDING to {}: {}", newStatus, allowed);
            return allowed;
        }
        
        // Permettre l'annulation depuis la plupart des statuts (sauf COMPLETED)
        if (newStatus == OrderStatus.CANCELLED) {
            boolean allowed = currentStatus != OrderStatus.COMPLETED && currentStatus != OrderStatus.CANCELLED;
            logger.info("🔍 DEBUG WEBHOOK - Cancellation from {}: {}", currentStatus, allowed);
            return allowed;
        }
        
        // Empêcher les retours en arrière inappropriés
        if (currentStatus == OrderStatus.COMPLETED) {
            logger.info("🔍 DEBUG WEBHOOK - Cannot modify completed order");
            return false; // Ne pas modifier les commandes déjà complétées
        }
        
        // Permettre les progressions normales
        logger.info("🔍 DEBUG WEBHOOK - Normal progression allowed");
        return true;
    }
}