package com.lmp.integration.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.domain.PaymentTransaction;
import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.domain.PaymentStatus;
import com.lmp.integration.domain.StripeWebhookEventType;
import com.lmp.integration.domain.WebhookEventLog;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.PaymentTransactionRepository;
import com.lmp.billing.repository.RefundRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.integration.repository.WebhookEventLogRepository;
import com.lmp.notification.service.EmailService;
import com.lmp.billing.service.InvoicePdfService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.lmp.billing.dto.WebhookEventDto;
import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.shared.util.VatIdentifierUtils;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.billing.exception.PaymentProcessingException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

/**
 * Gestionnaire des webhooks Stripe
 * Traite les événements asynchrones envoyés par Stripe
 * Assure la sécurité avec la vérification des signatures
 */
@Service
public class StripeWebhookHandler {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookHandler.class);
    private static final Logger securityLogger = LoggerFactory
            .getLogger("SECURITY." + StripeWebhookHandler.class.getName());

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

        private final PaymentTransactionRepository paymentTransactionRepository;

        private final OrderRepository orderRepository;

        private final UserRepository userRepository;

        private final EmailService emailService;

        private final InvoicePdfService invoicePdfService;

        private final WebhookEventLogRepository webhookEventLogRepository;

        private final TemplateEngine templateEngine;

        private final RefundRepository refundRepository;

        private final ApplicationEventPublisher eventPublisher;

        private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public StripeWebhookHandler(PaymentTransactionRepository paymentTransactionRepository,
                           OrderRepository orderRepository,
                           UserRepository userRepository,
                           EmailService emailService,
                           InvoicePdfService invoicePdfService,
                           WebhookEventLogRepository webhookEventLogRepository,
                           TemplateEngine templateEngine,
                           RefundRepository refundRepository,
                           ApplicationEventPublisher eventPublisher,
                           OrderRealtimeEventPublisher orderRealtimeEventPublisher) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.invoicePdfService = invoicePdfService;
        this.webhookEventLogRepository = webhookEventLogRepository;
        this.templateEngine = templateEngine;
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
    }

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

            // ── IDEMPOTENCE : vérifier si l'événement a déjà été traité ──
            Optional<WebhookEventLog> existingLog = webhookEventLogRepository.findByEventId(event.getId());
            if (existingLog.isPresent()) {
                String existingStatus = existingLog.get().getStatus();
                // Autoriser le retraitement des webhooks en échec (retry Stripe)
                if ("processed".equals(existingStatus)) {
                    logger.info("⏭️ IDEMPOTENCE - Événement déjà traité avec succès, ignoré: {} ({})",
                            event.getId(), event.getType());
                    WebhookEventDto duplicate = new WebhookEventDto(event.getId(), event.getType(), "stripe");
                    duplicate.setProcessed(true);
                    duplicate.setStatus("duplicate_skipped");
                    return duplicate;
                }
                // Supprimer le log en échec pour permettre le retraitement
                logger.info("🔄 RETRY - Événement précédemment en échec, retraitement autorisé: {} ({})",
                        event.getId(), event.getType());
                webhookEventLogRepository.delete(existingLog.get());
            }

            // Créer le DTO d'événement
            WebhookEventDto webhookEvent = new WebhookEventDto(
                    event.getId(),
                    event.getType(),
                    "stripe");
            webhookEvent.setEventTime(LocalDateTime.ofEpochSecond(event.getCreated(), 0, ZoneOffset.UTC));
            webhookEvent.setRawPayload(payload);
            webhookEvent.setSignature(sigHeader);

            // Traiter l'événement selon son type
            try {
                processEventByType(event, webhookEvent);
                webhookEvent.setProcessed(true);

                logger.info("Webhook event processed successfully - Event: {}, Type: {}",
                        event.getId(), event.getType());

                // ── IDEMPOTENCE : enregistrer l'événement comme traité ──
                WebhookEventLog eventLog = new WebhookEventLog(event.getId(), event.getType(), "stripe");
                eventLog.setStatus("processed");
                webhookEventLogRepository.save(eventLog);

            } catch (Exception e) {
                logger.error("Error processing webhook event {}: {}", event.getId(), e.getMessage(), e);
                webhookEvent.setProcessed(false);
                webhookEvent.setProcessingError(e.getMessage());

                // ── IDEMPOTENCE : enregistrer aussi les échecs pour éviter retries infinis ──
                try {
                    WebhookEventLog eventLog = new WebhookEventLog(event.getId(), event.getType(), "stripe");
                    eventLog.setStatus("failed");
                    eventLog.setProcessingError(e.getMessage());
                    webhookEventLogRepository.save(eventLog);
                } catch (Exception logEx) {
                    logger.error("Failed to log webhook event error: {}", logEx.getMessage());
                }

                securityLogger.warn("Webhook processing failed - Event: {}, Error: {}",
                        event.getId(), e.getMessage());
            }

            return webhookEvent;

        } catch (SignatureVerificationException e) {
            securityLogger.error("Webhook signature verification failed: {}", e.getMessage());
            throw new PaymentProcessingException(
                    "Signature de webhook invalide",
                    "INVALID_SIGNATURE",
                    "stripe");
        } catch (Exception e) {
            logger.error("Unexpected error processing webhook: {}", e.getMessage(), e);
            securityLogger.error("Unexpected webhook processing error: {}", e.getMessage());
            throw new PaymentProcessingException(
                    "Erreur lors du traitement du webhook",
                    "WEBHOOK_ERROR",
                    "stripe");
        }
    }

    /**
     * Traite les événements selon leur type en utilisant l'enum
     * StripeWebhookEventType
     */
    private void processEventByType(Event event, WebhookEventDto webhookEvent) {
        StripeWebhookEventType eventType = StripeWebhookEventType.fromStripeEventType(event.getType());

        if (eventType == null) {
            logger.info("Unhandled webhook event type: {}", event.getType());
            webhookEvent.setStatus("unhandled");
            return;
        }

        logger.info("🔍 DEBUG WEBHOOK - Processing event type: {} ({})", eventType, eventType.getDescription());

        switch (eventType) {
            case PAYMENT_INTENT_SUCCEEDED:
                handlePaymentIntentSucceeded(event, webhookEvent, eventType);
                break;

            case PAYMENT_INTENT_PAYMENT_FAILED:
                handlePaymentIntentFailed(event, webhookEvent, eventType);
                break;

            case PAYMENT_INTENT_REQUIRES_ACTION:
                handlePaymentIntentRequiresAction(event, webhookEvent, eventType);
                break;

            case CHARGE_DISPUTE_CREATED:
                handleChargeDisputeCreated(event, webhookEvent, eventType);
                break;

            case REFUND_CREATED:
                handleRefundCreated(event, webhookEvent, eventType);
                break;

            case REFUND_UPDATED:
                handleRefundUpdated(event, webhookEvent, eventType);
                break;

            case INVOICE_PAYMENT_SUCCEEDED:
                handleInvoicePaymentSucceeded(event, webhookEvent, eventType);
                break;

            case CHECKOUT_SESSION_COMPLETED:
                handleCheckoutSessionCompleted(event, webhookEvent, eventType);
                break;

            case CHECKOUT_SESSION_EXPIRED:
                handleCheckoutSessionExpired(event, webhookEvent, eventType);
                break;

            case CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED:
                handleCheckoutSessionAsyncPaymentSucceeded(event, webhookEvent, eventType);
                break;

            case CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED:
                handleCheckoutSessionAsyncPaymentFailed(event, webhookEvent, eventType);
                break;

            case CUSTOMER_SUBSCRIPTION_UPDATED:
                handleSubscriptionUpdated(event, webhookEvent, eventType);
                break;

            default:
                logger.warn("Event type {} is defined but not implemented in handler", eventType);
                webhookEvent.setStatus("unhandled");
        }
    }

    /**
     * Gère les paiements réussis en utilisant l'enum pour déterminer les statuts
     * cibles
     */
    private void handlePaymentIntentSucceeded(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

        if (paymentIntent != null) {
            logger.info("🔍 DEBUG WEBHOOK - handlePaymentIntentSucceeded called for PaymentIntent: {}",
                    paymentIntent.getId());
            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());
            logger.info("🔍 DEBUG WEBHOOK - PaymentIntent status: {}, amount: {}, currency: {}",
                    paymentIntent.getStatus(), paymentIntent.getAmount(), paymentIntent.getCurrency());
            logger.info("🔍 DEBUG WEBHOOK - PaymentIntent metadata: {}", paymentIntent.getMetadata());

            // 🆕 DIAGNOSTIC AVANCÉ - Vérification des liens Order
            logger.info("🔍 DIAGNOSTIC LINKS - Searching for order by PaymentIntent ID: {}", paymentIntent.getId());
            Optional<Order> orderByPI = orderRepository.findByStripePaymentIntentId(paymentIntent.getId());
            logger.info("🔍 DIAGNOSTIC LINKS - Order found by PaymentIntent: {}", orderByPI.isPresent());

            if (orderByPI.isEmpty() && paymentIntent.getMetadata() != null) {
                String sessionId = paymentIntent.getMetadata().get("session_id");
                logger.info("🔍 DIAGNOSTIC LINKS - Trying fallback search by session_id: {}", sessionId);
                if (sessionId != null) {
                    Optional<Order> orderBySession = orderRepository.findByStripeSessionId(sessionId);
                    logger.info("🔍 DIAGNOSTIC LINKS - Order found by session_id: {}", orderBySession.isPresent());
                    if (orderBySession.isPresent()) {
                        Order order = orderBySession.get();
                        logger.info(
                                "🔍 DIAGNOSTIC LINKS - Found order {}, current paymentIntentId: {}, will update to: {}",
                                order.getId(), order.getStripePaymentIntentId(), paymentIntent.getId());
                    }
                }
            }

            webhookEvent.setProviderTransactionId(paymentIntent.getId());
            webhookEvent.setStatus("succeeded");

            // Utiliser les statuts définis dans l'enum
            if (eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(paymentIntent.getId(), eventType.getTargetPaymentStatus());
            }

            // Mettre à jour le statut de l'Order selon l'enum
            if (eventType.shouldUpdateOrderStatus()) {
                updateOrderStatusByPaymentIntentId(paymentIntent.getId(), eventType.getTargetOrderStatus(),
                        paymentIntent);
            }

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
     * Gère les échecs de paiement en utilisant l'enum
     */
    private void handlePaymentIntentFailed(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

        if (paymentIntent != null) {
            webhookEvent.setProviderTransactionId(paymentIntent.getId());
            webhookEvent.setStatus("failed");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());

            // Utiliser les statuts définis dans l'enum
            if (eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(paymentIntent.getId(), eventType.getTargetPaymentStatus());
            }

            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("amount", paymentIntent.getAmount());
            eventData.put("currency", paymentIntent.getCurrency());
            eventData.put("last_payment_error", paymentIntent.getLastPaymentError());
            webhookEvent.setEventData(eventData);

            logger.warn("Payment failed - PaymentIntent: {}, Error: {}",
                    paymentIntent.getId(), paymentIntent.getLastPaymentError());

            securityLogger.info("Payment failure recorded - PaymentIntent: {}", paymentIntent.getId());

            publishPaymentFailed(paymentIntent);
        }
    }

    /**
     * Gère les paiements nécessitant une action supplémentaire en utilisant l'enum
     */
    private void handlePaymentIntentRequiresAction(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

        if (paymentIntent != null) {
            webhookEvent.setProviderTransactionId(paymentIntent.getId());
            webhookEvent.setStatus("requires_action");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());

            // Utiliser les statuts définis dans l'enum
            if (eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(paymentIntent.getId(), eventType.getTargetPaymentStatus());
            }

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
     * Gère la création de litiges en utilisant l'enum
     */
    private void handleChargeDisputeCreated(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        // Traitement des litiges - notification aux administrateurs
        webhookEvent.setStatus("dispute_created");

        logger.warn("Charge dispute created - Event: {} ({})", event.getId(), eventType.getDescription());
        securityLogger.warn("Payment dispute created - Event: {}", event.getId());

        // TODO: Implémenter la notification aux administrateurs
        // TODO: Utiliser eventType.getTargetOrderStatus() pour mettre à jour la
        // commande
    }

    /**
     * Gère la création de remboursements en utilisant l'enum
     */
    private void handleRefundCreated(Event event, WebhookEventDto webhookEvent, StripeWebhookEventType eventType) {
        Refund stripeRefund = (Refund) event.getDataObjectDeserializer().getObject().orElse(null);

        if (stripeRefund != null) {
            webhookEvent.setProviderTransactionId(stripeRefund.getPaymentIntent());
            webhookEvent.setStatus("refund_created");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());

            // 🆕 PERSIST REFUND: Save the refund entity linked to the order
            persistRefundFromStripe(stripeRefund);

            // Ajouter les données de l'événement
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("refund_id", stripeRefund.getId());
            eventData.put("amount", stripeRefund.getAmount());
            eventData.put("currency", stripeRefund.getCurrency());
            eventData.put("reason", stripeRefund.getReason());
            webhookEvent.setEventData(eventData);

            logger.info("Refund created - Refund: {}, Amount: {} {}",
                    stripeRefund.getId(), stripeRefund.getAmount(), stripeRefund.getCurrency());
        }
    }

    /**
     * 🆕 NOUVEAUTÉ : Persiste un remboursement Stripe en base de données.
     * Recherche la commande associée et crée un enregistrement Refund.
     */
    private void persistRefundFromStripe(Refund stripeRefund) {
        try {
            // Check idempotency — avoid duplicates
            Optional<com.lmp.billing.domain.Refund> existing = refundRepository.findByStripeRefundId(stripeRefund.getId());
            if (existing.isPresent()) {
                logger.info("⏭️ Refund already persisted: {}", stripeRefund.getId());
                return;
            }

            // Find the order by PaymentIntent ID
            Optional<Order> orderOpt = Optional.empty();
            if (stripeRefund.getPaymentIntent() != null) {
                orderOpt = orderRepository.findByStripePaymentIntentId(stripeRefund.getPaymentIntent());
            }

            if (orderOpt.isEmpty()) {
                logger.warn("❌ REFUND PERSIST - Commande non trouvée pour PaymentIntent: {}", stripeRefund.getPaymentIntent());
                return;
            }

            Order order = orderOpt.get();
            OrderStatus statusBeforeRefund = order.getStatus();

            com.lmp.billing.domain.Refund refundEntity = new com.lmp.billing.domain.Refund();
            refundEntity.setOrder(order);
            refundEntity.setStripeRefundId(stripeRefund.getId());
            refundEntity.setAmount(BigDecimal.valueOf(stripeRefund.getAmount()).divide(BigDecimal.valueOf(100)));
            refundEntity.setCurrency(stripeRefund.getCurrency());
            refundEntity.setReason(stripeRefund.getReason());
            refundEntity.setStatus(stripeRefund.getStatus() != null ? stripeRefund.getStatus() : "pending");
            refundEntity.setCreatedAt(LocalDateTime.now());
            refundEntity.setProcessedBy("stripe_webhook");

            refundRepository.save(refundEntity);

            Map<String, Object> rpl = new HashMap<>();
            rpl.put("orderId", order.getId().toString());
            if (order.getUser() != null) {
                rpl.put("userId", order.getUser().getId().toString());
            }
            rpl.put("stripeRefundId", stripeRefund.getId());
            rpl.put("amount", refundEntity.getAmount().doubleValue());
            rpl.put("currency", refundEntity.getCurrency());
            rpl.put(BusinessEventPayloadKeys.MESSAGE, String.format("Remboursement enregistré — commande %s",
                    order.getId()));
            eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.REFUND_PROCESSED, "billing", order.getId(), rpl));

            logger.info("✅ REFUND PERSISTED - ID: {}, Stripe: {}, Montant: {} {}, Commande: {}",
                    refundEntity.getId(), stripeRefund.getId(),
                    refundEntity.getAmount(), refundEntity.getCurrency(), order.getId());

            // Update order status if full refund
            BigDecimal totalRefunded = refundRepository.getTotalRefundedAmountByOrderId(order.getId());
            if (totalRefunded.compareTo(order.getTotalAmount()) >= 0) {
                order.setStatus(OrderStatus.REFUNDED);
                order.setUpdatedAt(LocalDateTime.now());
                OrderProgressSync.applyMinimumForStatus(order);
                orderRepository.save(order);
                orderRealtimeEventPublisher.publishOrderUpdated(order, statusBeforeRefund, OrderStatus.REFUNDED);
                logger.info("🔄 REFUND - Commande {} marquée comme REFUNDED (remboursement total)", order.getId());
            }

        } catch (Exception e) {
            logger.error("❌ REFUND PERSIST ERROR - Erreur lors de la persistance du remboursement {}: {}",
                    stripeRefund.getId(), e.getMessage(), e);
        }
    }

    /**
     * Gère la mise à jour de remboursements en utilisant l'enum
     */
    private void handleRefundUpdated(Event event, WebhookEventDto webhookEvent, StripeWebhookEventType eventType) {
        Refund stripeRefund = (Refund) event.getDataObjectDeserializer().getObject().orElse(null);

        if (stripeRefund != null) {
            webhookEvent.setProviderTransactionId(stripeRefund.getPaymentIntent());
            webhookEvent.setStatus("refund_updated");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());

            // Update persisted refund status
            Optional<com.lmp.billing.domain.Refund> refundOpt = refundRepository.findByStripeRefundId(stripeRefund.getId());
            if (refundOpt.isPresent()) {
                com.lmp.billing.domain.Refund refundEntity = refundOpt.get();
                refundEntity.setStatus(stripeRefund.getStatus());
                if ("succeeded".equals(stripeRefund.getStatus())) {
                    refundEntity.setProcessedAt(LocalDateTime.now());
                } else if ("failed".equals(stripeRefund.getStatus())) {
                    refundEntity.setFailureReason(stripeRefund.getFailureReason() != null
                            ? stripeRefund.getFailureReason().toString() : "Unknown");
                }
                refundRepository.save(refundEntity);
                logger.info("✅ REFUND UPDATED - Stripe: {}, Status: {}", stripeRefund.getId(), stripeRefund.getStatus());

                Map<String, Object> upl = new HashMap<>();
                upl.put("stripeRefundId", stripeRefund.getId());
                upl.put("status", stripeRefund.getStatus());
                Order linkedOrder = refundEntity.getOrder();
                upl.put("orderId", linkedOrder.getId().toString());
                upl.put(BusinessEventPayloadKeys.MESSAGE, String.format("Mise à jour remboursement — %s",
                        stripeRefund.getId()));
                eventPublisher.publishEvent(
                        LmpBusinessEvent.of(EventType.REFUND_STATUS_UPDATED, "billing", linkedOrder.getId(), upl));
            }

            // Mettre à jour le statut si le remboursement est réussi
            if ("succeeded".equals(stripeRefund.getStatus()) && eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(stripeRefund.getPaymentIntent(), eventType.getTargetPaymentStatus());
            }

            logger.info("Refund updated - Refund: {}, Status: {}", stripeRefund.getId(), stripeRefund.getStatus());
        }
    }

    /**
     * Gère les paiements de facture réussis en utilisant l'enum
     */
    private void handleInvoicePaymentSucceeded(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        webhookEvent.setStatus("invoice_paid");
        logger.info("Invoice payment succeeded - Event: {} ({})", event.getId(), eventType.getDescription());
    }

    /**
     * Gère la completion des sessions Checkout en utilisant l'enum
     * 🆕 AMÉLIORÉ : Logique hybride pour mise à jour d'ordres existants au lieu de
     * créer de nouveaux
     */
    private void handleCheckoutSessionCompleted(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        // 🔧 NOUVELLE APPROCHE : Désérialisation manuelle robuste
        Session session = null;

        // 1. Tentative de désérialisation standard
        try {
            Optional<com.stripe.model.StripeObject> objectOpt = event.getDataObjectDeserializer().getObject();
            if (objectOpt.isPresent() && objectOpt.get() instanceof Session) {
                session = (Session) objectOpt.get();
                logger.info("✅ DÉSÉRIALISATION STANDARD - Session extraite avec succès: {}", session.getId());
            }
        } catch (Exception e) {
            logger.warn("⚠️ DÉSÉRIALISATION STANDARD ÉCHOUÉE - {}", e.getMessage());
        }

        // 2. Si échec, désérialisation manuelle du JSON
        if (session == null) {
            logger.info("🔧 DÉSÉRIALISATION MANUELLE - Tentative de parsing JSON direct");
            session = parseSessionFromJson(event);
        }

        if (session != null) {
            // 🔍 DIAGNOSTIC - Analyser les métadonnées
            logger.info("🔍 DIAGNOSTIC - Session ID: {}", session.getId());
            logger.info("🔍 DIAGNOSTIC - Session metadata: {}", session.getMetadata());
            logger.info("🔍 DIAGNOSTIC - Session customer_email: {}", session.getCustomerEmail());
            logger.info("🔍 DIAGNOSTIC - Session amount_total: {}", session.getAmountTotal());
            logger.info("🔍 DIAGNOSTIC - Session currency: {}", session.getCurrency());
            logger.info("🔍 DIAGNOSTIC - Session payment_intent: {}", session.getPaymentIntent());
            logger.info("🔍 DIAGNOSTIC - Session payment_status: {}", session.getPaymentStatus());
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("completed");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());
            logger.info("🔍 DEBUG WEBHOOK - Checkout completed for session: {}", session.getId());

            // 🔧 AMÉLIORATION : Utiliser la logique pour trouver et mettre à jour l'ordre
            // existant
            // Priorité : 1) PaymentIntent ID, 2) Session ID, 3) Metadata order_id
            Optional<Order> orderOpt = findOrderBySession(session);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                logger.info("📝 MISE À JOUR EXISTANTE - Commande trouvée: {} pour la session {}, mise à jour du statut",
                        order.getId(), session.getId());

                // Mettre à jour l'ordre existant avec les informations de la session
                updateOrderWithSessionData(order, session);

                // Utiliser les statuts définis dans l'enum
                if (eventType.shouldUpdatePaymentStatus()) {
                    updatePaymentTransactionStatus(session.getPaymentIntent(), eventType.getTargetPaymentStatus());
                }

                // Mettre à jour le statut de l'Order selon l'enum
                if (eventType.shouldUpdateOrderStatus()) {
                    OrderStatus newStatus = eventType.getTargetOrderStatus();
                    updateOrderStatus(order, newStatus, session);
                }

            } else {
                // 🆕 CRÉATION : Si aucune commande existante n'est trouvée, créer une nouvelle
                // commande
                // Cela ne devrait se produire que dans les cas de fallback ou erreurs dans le
                // workflow
                logger.warn(
                        "🆕 CRÉATION FONCTION DE SECOURS - Aucune commande trouvée pour session {}, création via webhook",
                        session.getId());
                Order newOrder = createOrderFromCheckoutSession(session);
                if (newOrder != null) {
                    logger.info("✅ CRÉATION RÉUSSIE - Commande {} créée via fonction de secours pour session {}",
                            newOrder.getId(), session.getId());
                } else {
                    logger.error(
                            "❌ ÉCHEC CRÉATION - Impossible de créer commande pour session {} via fonction de secours",
                            session.getId());
                }
            }

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
        } else {
            logger.error("❌ Session Stripe null dans handleCheckoutSessionCompleted");
        }
    }

    /**
     * Gère l'expiration des sessions Checkout en utilisant l'enum
     */
    private void handleCheckoutSessionExpired(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("expired");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());

            // Utiliser les statuts définis dans l'enum
            if (eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(session.getId(), eventType.getTargetPaymentStatus());
            }

            // Mettre à jour le statut de l'Order selon l'enum
            if (eventType.shouldUpdateOrderStatus()) {
                updateOrderStatusByStripeSessionId(session.getId(), eventType.getTargetOrderStatus());
            }

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
     * Gère les paiements asynchrones réussis (ex: virements bancaires) en utilisant
     * l'enum
     */
    private void handleCheckoutSessionAsyncPaymentSucceeded(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("async_payment_succeeded");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());
            logger.info("🔍 DEBUG WEBHOOK - Async payment succeeded, updating order status for session: {}",
                    session.getId());

            // Utiliser les statuts définis dans l'enum
            if (eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(session.getId(), eventType.getTargetPaymentStatus());
            }

            // Mettre à jour le statut de l'Order selon l'enum
            if (eventType.shouldUpdateOrderStatus()) {
                updateOrderStatusByStripeSessionId(session.getId(), eventType.getTargetOrderStatus());
            }

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
     * Gère les échecs de paiements asynchrones en utilisant l'enum
     */
    private void handleCheckoutSessionAsyncPaymentFailed(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

        if (session != null) {
            webhookEvent.setProviderTransactionId(session.getId());
            webhookEvent.setStatus("async_payment_failed");

            logger.info("🔍 DEBUG WEBHOOK - Event: {} ({})", eventType, eventType.getDescription());

            // Utiliser les statuts définis dans l'enum
            if (eventType.shouldUpdatePaymentStatus()) {
                updatePaymentTransactionStatus(session.getId(), eventType.getTargetPaymentStatus());
            }

            // Mettre à jour le statut de l'Order selon l'enum
            if (eventType.shouldUpdateOrderStatus()) {
                updateOrderStatusByStripeSessionId(session.getId(), eventType.getTargetOrderStatus());
            }

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
     * Gère les mises à jour d'abonnement en utilisant l'enum
     */
    private void handleSubscriptionUpdated(Event event, WebhookEventDto webhookEvent,
            StripeWebhookEventType eventType) {
        webhookEvent.setStatus("subscription_updated");
        logger.info("Subscription updated - Event: {} ({})", event.getId(), eventType.getDescription());
    }

    /**
     * Met à jour le statut d'une transaction de paiement
     */
    private void updatePaymentTransactionStatus(String providerTransactionId, PaymentStatus status) {
        try {
            Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository
                    .findByTransactionId(providerTransactionId);

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
     * Cette méthode est cruciale pour résoudre le problème de persistance
     * prématurée des commandes
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
                        order.setCheckoutToken(null);
                        // Auto-verify user
                        if (order.getUser() != null) {
                            autoVerifyUserOnPayment(order.getUser());
                        }
                    }
                    OrderProgressSync.applyMinimumForStatus(order);

                    orderRepository.save(order);

                    orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, oldStatus, newStatus);

                    logger.info("✅ Order status updated via webhook - Order: {}, Stripe Session: {}, Status: {} -> {}",
                            order.getId(), stripeSessionId, oldStatus, newStatus);

                    securityLogger.info("Order status updated via Stripe webhook - Order: {}, Session: {}",
                            order.getId(), stripeSessionId);
                } else {
                    logger.warn(
                            "❌ Order status update skipped - Order: {}, Current: {}, Requested: {}, Reason: Invalid transition",
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
     * Cette méthode recherche la commande par PaymentIntent et met à jour son
     * statut
     */
    private void updateOrderStatusByPaymentIntentId(String paymentIntentId, OrderStatus newStatus,
            PaymentIntent paymentIntent) {
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
                        if (order.getPaymentMethod() == null || order.getPaymentMethod().isBlank()) {
                            order.setPaymentMethod("stripe_payment_intent");
                        }
                        order.setCheckoutToken(null);
                        // Auto-verify user
                        if (order.getUser() != null) {
                            autoVerifyUserOnPayment(order.getUser());
                        }
                    }
                    OrderProgressSync.applyMinimumForStatus(order);

                    orderRepository.save(order);

                    orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, oldStatus, newStatus);

                    if (newStatus == OrderStatus.CONFIRMED && order.getUser() != null) {
                        sendInvoiceByEmail(order, order.getUser());
                    }

                    logger.info(
                            "✅ Order status updated via PaymentIntent webhook - Order: {}, PaymentIntent: {}, Status: {} -> {}",
                            order.getId(), paymentIntentId, oldStatus, newStatus);

                    securityLogger.info(
                            "Order status updated via Stripe PaymentIntent webhook - Order: {}, PaymentIntent: {}",
                            order.getId(), paymentIntentId);
                } else {
                    logger.warn(
                            "❌ Order status update skipped - Order: {}, Current: {}, Requested: {}, Reason: Invalid transition",
                            order.getId(), oldStatus, newStatus);
                }
            } else {
                logger.warn("❌ Order not found for PaymentIntent ID: {}", paymentIntentId);

                // Tentative de recherche par metadata session_id
                if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("session_id")) {
                    String sessionId = paymentIntent.getMetadata().get("session_id");
                    logger.info("🔍 DEBUG WEBHOOK - Trying alternative search by session_id from metadata: {}",
                            sessionId);
                    updateOrderStatusByStripeSessionId(sessionId, newStatus);
                } else {
                    logger.error("❌ No session_id in PaymentIntent metadata, cannot find order - PaymentIntent: {}",
                            paymentIntentId);

                    // Log toutes les commandes existantes pour debug
                    List<Order> allOrders = orderRepository.findAll();
                    logger.info("🔍 DEBUG WEBHOOK - Total orders in database: {}", allOrders.size());
                    for (Order o : allOrders) {
                        logger.info(
                                "🔍 DEBUG WEBHOOK - Order {}: stripePaymentIntentId={}, stripeSessionId={}, status={}",
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

        // 🆕 DIAGNOSTIC DÉTAILLÉ - Analyser toutes les transitions possibles
        if (currentStatus == newStatus) {
            logger.info("🔍 DIAGNOSTIC STATUS - Status unchanged: {}, skipping update", currentStatus);
            return false;
        }

        // Permettre les transitions depuis PAYMENT_PENDING
        if (currentStatus == OrderStatus.PAYMENT_PENDING) {
            boolean allowed = newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.PENDING
                    || newStatus == OrderStatus.CANCELLED;
            logger.info("🔍 DIAGNOSTIC STATUS - From PAYMENT_PENDING to {}: {}", newStatus, allowed);
            if (!allowed) {
                logger.warn("🔍 DIAGNOSTIC STATUS - BLOCKED: Invalid transition from PAYMENT_PENDING to {}", newStatus);
            }
            return allowed;
        }

        // Permettre la récupération d'une commande annulée si le paiement est confirmé par Stripe
        if (currentStatus == OrderStatus.CANCELLED && newStatus == OrderStatus.CONFIRMED) {
            logger.info("🔄 RÉCUPÉRATION - Commande annulée récupérée car paiement confirmé par Stripe");
            return true;
        }

        // Transitions depuis PENDING
        if (currentStatus == OrderStatus.PENDING) {
            boolean allowed = newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED ||
                    newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.SHIPPED;
            logger.info("🔍 DIAGNOSTIC STATUS - From PENDING to {}: {}", newStatus, allowed);
            return allowed;
        }

        // Permettre l'annulation depuis la plupart des statuts (sauf COMPLETED)
        if (newStatus == OrderStatus.CANCELLED) {
            boolean allowed = currentStatus != OrderStatus.COMPLETED && currentStatus != OrderStatus.CANCELLED;
            logger.info("🔍 DIAGNOSTIC STATUS - Cancellation from {}: {}", currentStatus, allowed);
            if (!allowed) {
                logger.warn("🔍 DIAGNOSTIC STATUS - BLOCKED: Cannot cancel order with status {}", currentStatus);
            }
            return allowed;
        }

        // Empêcher les retours en arrière inappropriés
        if (currentStatus == OrderStatus.COMPLETED) {
            logger.warn("🔍 DIAGNOSTIC STATUS - BLOCKED: Cannot modify completed order");
            return false; // Ne pas modifier les commandes déjà complétées
        }

        // Transitions vers CONFIRMED depuis d'autres statuts
        if (newStatus == OrderStatus.CONFIRMED) {
            boolean allowed = currentStatus == OrderStatus.PAYMENT_PENDING || currentStatus == OrderStatus.PENDING ||
                    currentStatus == OrderStatus.PROCESSING;
            logger.info("🔍 DIAGNOSTIC STATUS - To CONFIRMED from {}: {}", currentStatus, allowed);
            return allowed;
        }

        // Permettre les progressions normales
        logger.info("🔍 DIAGNOSTIC STATUS - Normal progression allowed: {} -> {}", currentStatus, newStatus);
        return true;
    }

    /**
     * 🆕 PHASE 1 : Crée une nouvelle commande depuis une session Stripe checkout
     * Cette méthode extrait les métadonnées de la session et crée une commande
     * CONFIRMÉE
     */
    private Order createOrderFromCheckoutSession(Session session) {
        try {
            logger.info("🆕 CRÉATION COMMANDE - Début création pour session: {}", session.getId());

            // 1. Extraire les métadonnées requises
            Map<String, String> metadata = session.getMetadata();
            if (metadata == null || metadata.isEmpty()) {
                logger.error("❌ MÉTADONNÉES MANQUANTES - Session {} sans métadonnées", session.getId());
                return null;
            }

            // 2. Extraire les informations requises
            String serviceName = metadata.get("serviceName");
            String amountStr = metadata.get("amount");
            String currency = metadata.get("currency");
            String userIdStr = metadata.get("userId");

            logger.info("🔍 MÉTADONNÉES EXTRAITES - Service: {}, Amount: {}, Currency: {}, UserId: {}",
                    serviceName, amountStr, currency, userIdStr);

            // 3. Valider les données essentielles
            if (serviceName == null || serviceName.trim().isEmpty()) {
                logger.error("❌ SERVICE MANQUANT - serviceName requis dans métadonnées");
                return null;
            }

            if (amountStr == null || amountStr.trim().isEmpty()) {
                logger.error("❌ MONTANT MANQUANT - amount requis dans métadonnées");
                return null;
            }

            // 4. Convertir le montant
            BigDecimal amount;
            try {
                // Le montant Stripe est en centimes, on le convertit en dollars
                Long amountCents = Long.parseLong(amountStr);
                amount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
            } catch (NumberFormatException e) {
                logger.error("❌ MONTANT INVALIDE - Impossible de parser amount: {}", amountStr, e);
                return null;
            }

            // 5. Trouver l'utilisateur
            User user = null;
            if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                try {
                    java.util.UUID userId = java.util.UUID.fromString(userIdStr);
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        user = userOpt.get();
                        logger.info("✅ UTILISATEUR TROUVÉ - ID: {}, Email: {}", userId, user.getEmail());
                    } else {
                        logger.warn("⚠️ UTILISATEUR NON TROUVÉ - ID: {}", userId);
                    }
                } catch (NumberFormatException e) {
                    logger.error("❌ USER_ID INVALIDE - Impossible de parser userId: {}", userIdStr, e);
                }
            }

            // Si pas d'utilisateur trouvé par ID, essayer par email
            if (user == null && session.getCustomerEmail() != null) {
                Optional<User> userOpt = userRepository.findByEmail(session.getCustomerEmail());
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    logger.info("✅ UTILISATEUR TROUVÉ PAR EMAIL - Email: {}", session.getCustomerEmail());
                } else {
                    logger.warn("⚠️ UTILISATEUR NON TROUVÉ PAR EMAIL - Email: {}", session.getCustomerEmail());
                }
            }

            // 6. Créer la nouvelle commande
            Order newOrder = new Order();
            newOrder.setUser(user);
            newOrder.setServiceName(serviceName.trim());
            newOrder.setTotalAmount(amount);
            newOrder.setCurrency(currency != null ? currency : "EUR");
            newOrder.setStatus(OrderStatus.CONFIRMED); // Statut CONFIRMÉ directement
            newOrder.setPaymentStatus("succeeded");

            // Informations Stripe
            newOrder.setStripeSessionId(session.getId());
            newOrder.setStripePaymentIntentId(session.getPaymentIntent());
            newOrder.setStripeCustomerId(session.getCustomer());
            newOrder.setPaymentMethod("stripe_checkout");

            if (user != null) {
                boolean reverse = Boolean.TRUE.equals(user.getVatReverseCharge());
                newOrder.setVatReverseCharge(reverse);
                String vat = VatIdentifierUtils.normalize(user.getVatNumber());
                newOrder.setCustomerVatNumber(reverse && !vat.isEmpty() ? vat : null);
            }

            // Dates importantes
            LocalDateTime now = LocalDateTime.now();
            newOrder.setCreatedAt(now);
            newOrder.setUpdatedAt(now);
            newOrder.setPaidAt(now); // Payé immédiatement

            // Informations de facturation si disponibles
            if (session.getCustomerEmail() != null) {
                // Stocker l'email dans les notes si pas d'utilisateur associé
                if (user == null) {
                    newOrder.setNotes("Email client: " + session.getCustomerEmail());
                }
            }

            OrderProgressSync.applyMinimumForStatus(newOrder);

            // 7. Sauvegarder la commande
            Order savedOrder = orderRepository.save(newOrder);

            logger.info("✅ COMMANDE CRÉÉE - ID: {}, Session: {}, Service: {}, Montant: {} {}",
                    savedOrder.getId(), session.getId(), serviceName, amount, currency);

            securityLogger.info("Order created via Stripe webhook - Order: {}, Session: {}, Amount: {} {}",
                    savedOrder.getId(), session.getId(), amount, currency);

            orderRealtimeEventPublisher.publishPaymentReceived(savedOrder);

            return savedOrder;

        } catch (Exception e) {
            logger.error("❌ ERREUR CRÉATION COMMANDE - Session: {}, Erreur: {}", session.getId(), e.getMessage(), e);
            securityLogger.error("Order creation failed via webhook - Session: {}, Error: {}", session.getId(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * 🔧 NOUVELLE MÉTHODE : Désérialisation manuelle robuste du JSON Session
     * Cette méthode parse directement le JSON pour extraire les informations
     * nécessaires
     * Compatible avec toutes les versions d'API Stripe
     */
    private Session parseSessionFromJson(Event event) {
        try {
            logger.info("🔧 PARSING JSON - Début de la désérialisation manuelle");

            // 1. Extraire le JSON brut de l'événement
            JsonNode eventData = objectMapper.readTree(event.getData().toJson());
            JsonNode sessionJson = eventData.get("object");

            if (sessionJson == null) {
                logger.error("❌ JSON PARSING - Pas d'objet 'object' dans les données");
                return null;
            }

            logger.info("🔧 JSON STRUCTURE - Object type: {}", sessionJson.get("object").asText());

            // 2. Vérifier que c'est bien une session checkout
            if (!"checkout.session".equals(sessionJson.get("object").asText())) {
                logger.error("❌ JSON PARSING - L'objet n'est pas une checkout.session: {}",
                        sessionJson.get("object").asText());
                return null;
            }

            // 3. Créer un objet Session avec les données essentielles
            Session manualSession = new Session();

            // ID de session
            if (sessionJson.has("id")) {
                manualSession.setId(sessionJson.get("id").asText());
                logger.info("✅ JSON PARSED - Session ID: {}", manualSession.getId());
            }

            // PaymentIntent
            if (sessionJson.has("payment_intent")) {
                String paymentIntentId = sessionJson.get("payment_intent").asText();
                manualSession.setPaymentIntent(paymentIntentId);
                logger.info("✅ JSON PARSED - Payment Intent: {}", paymentIntentId);
            }

            // Customer email
            if (sessionJson.has("customer_email") && !sessionJson.get("customer_email").isNull()) {
                manualSession.setCustomerEmail(sessionJson.get("customer_email").asText());
                logger.info("✅ JSON PARSED - Customer Email: {}", manualSession.getCustomerEmail());
            }

            // Customer details - email alternatif
            if (sessionJson.has("customer_details")) {
                JsonNode customerDetails = sessionJson.get("customer_details");
                if (customerDetails.has("email") && !customerDetails.get("email").isNull()) {
                    String email = customerDetails.get("email").asText();
                    if (manualSession.getCustomerEmail() == null) {
                        manualSession.setCustomerEmail(email);
                        logger.info("✅ JSON PARSED - Customer Email (from details): {}", email);
                    }
                }
            }

            // Montant total
            if (sessionJson.has("amount_total")) {
                Long amountTotal = sessionJson.get("amount_total").asLong();
                manualSession.setAmountTotal(amountTotal);
                logger.info("✅ JSON PARSED - Amount Total: {}", amountTotal);
            }

            // Devise
            if (sessionJson.has("currency")) {
                manualSession.setCurrency(sessionJson.get("currency").asText());
                logger.info("✅ JSON PARSED - Currency: {}", manualSession.getCurrency());
            }

            // Statut de paiement
            if (sessionJson.has("payment_status")) {
                manualSession.setPaymentStatus(sessionJson.get("payment_status").asText());
                logger.info("✅ JSON PARSED - Payment Status: {}", manualSession.getPaymentStatus());
            }

            // Customer ID
            if (sessionJson.has("customer") && !sessionJson.get("customer").isNull()) {
                manualSession.setCustomer(sessionJson.get("customer").asText());
                logger.info("✅ JSON PARSED - Customer ID: {}", manualSession.getCustomer());
            }

            // Métadonnées (crucial pour la création de commandes)
            if (sessionJson.has("metadata")) {
                JsonNode metadataNode = sessionJson.get("metadata");
                Map<String, String> metadata = new HashMap<>();

                metadataNode.fields().forEachRemaining(entry -> {
                    metadata.put(entry.getKey(), entry.getValue().asText());
                });

                manualSession.setMetadata(metadata);
                logger.info("✅ JSON PARSED - Metadata: {}", metadata);
            }

            logger.info("✅ DÉSÉRIALISATION MANUELLE RÉUSSIE - Session: {}", manualSession.getId());
            return manualSession;

        } catch (Exception e) {
            logger.error("❌ ÉCHEC DÉSÉRIALISATION MANUELLE - Erreur: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🆕 NOUVEAUTÉ : Trouve une commande existante liée à la session Stripe
     * Priorité : 1) PaymentIntent ID, 2) Session ID, 3) Metadata order_id
     */
    private Optional<Order> findOrderBySession(Session session) {
        logger.info("🔍 RECHERCHE COMMANDE - Recherche d'une commande existante pour la session: {}", session.getId());

        // 1. Recherche par PaymentIntent ID (le plus fiable)
        if (session.getPaymentIntent() != null) {
            Optional<Order> orderByPI = orderRepository.findByStripePaymentIntentId(session.getPaymentIntent());
            if (orderByPI.isPresent()) {
                logger.info("✅ TROUVÉ PAR PAYMENT_INTENT - Commande {} trouvée pour PaymentIntent: {}",
                        orderByPI.get().getId(), session.getPaymentIntent());
                return orderByPI;
            }
        }

        // 2. Recherche par Session ID
        Optional<Order> orderBySession = orderRepository.findByStripeSessionId(session.getId());
        if (orderBySession.isPresent()) {
            logger.info("✅ TROUVÉ PAR SESSION_ID - Commande {} trouvée pour Session: {}",
                    orderBySession.get().getId(), session.getId());
            return orderBySession;
        }

        // 3. Recherche par metadata order_id (fallback)
        if (session.getMetadata() != null && session.getMetadata().containsKey("order_id")) {
            String orderIdStr = session.getMetadata().get("order_id");
            try {
                java.util.UUID orderId = java.util.UUID.fromString(orderIdStr);
                Optional<Order> orderByMetadata = orderRepository.findById(orderId);
                if (orderByMetadata.isPresent()) {
                    logger.info("✅ TROUVÉ PAR METADATA - Commande {} trouvée via metadata order_id: {}",
                            orderByMetadata.get().getId(), orderId);
                    return orderByMetadata;
                }
            } catch (NumberFormatException e) {
                logger.warn("⚠️ METADATA ORDER_ID INVALIDE - Impossible de parser order_id: {}", orderIdStr);
            }
        }

        logger.warn("❌ AUCUNE COMMANDE TROUVÉE - Aucune commande existante trouvée pour la session: {}",
                session.getId());
        return Optional.empty();
    }

    /**
     * 🆕 NOUVEAUTÉ : Met à jour une commande existante avec les données de la
     * session Stripe
     */
    private void updateOrderWithSessionData(Order order, Session session) {
        logger.info("🔄 MISE À JOUR COMMANDE - Mise à jour de la commande {} avec les données de la session {}",
                order.getId(), session.getId());

        // Mise à jour des informations de paiement
        order.setStripeSessionId(session.getId());
        if (session.getPaymentIntent() != null) {
            order.setStripePaymentIntentId(session.getPaymentIntent());
        }
        if (session.getCustomer() != null) {
            order.setStripeCustomerId(session.getCustomer());
        }
        // Mapper le statut Stripe vers le statut interne : "paid" → "succeeded"
        String internalPaymentStatus = "paid".equals(session.getPaymentStatus()) ? "succeeded" : session.getPaymentStatus();
        order.setPaymentStatus(internalPaymentStatus);
        order.setPaymentMethod("stripe_checkout");

        // Mise à jour des informations de facturation si disponibles
        if (session.getCustomerEmail() != null) {
            // Si l'utilisateur n'est pas encore associé et qu'on a un email correspondant
            if (order.getUser() == null) {
                Optional<User> userOpt = userRepository.findByEmail(session.getCustomerEmail());
                if (userOpt.isPresent()) {
                    order.setUser(userOpt.get());
                    logger.info("👤 UTILISATEUR ASSOCIÉ - Utilisateur {} associé via email: {}",
                            userOpt.get().getId(), session.getCustomerEmail());
                }
            }
        }

        // Mise à jour des dates importantes
        order.setUpdatedAt(LocalDateTime.now());
        if ("paid".equals(session.getPaymentStatus())) {
            order.setPaidAt(LocalDateTime.now());
        } else if ("unpaid".equals(session.getPaymentStatus()) || "failed".equals(session.getPaymentStatus())) {
            order.setPaidAt(null);
        }

        // Sauvegarde des modifications
        orderRepository.save(order);

        logger.info("✅ MISE À JOUR COMMANDE TERMINÉE - Commande {} mise à jour avec succès", order.getId());
    }

    /**
     * 🆕 NOUVEAUTÉ : Met à jour le statut de la commande avec validation de
     * transition logique
     */
    private void updateOrderStatus(Order order, OrderStatus newStatus, Session session) {
        OrderStatus oldStatus = order.getStatus();
        logger.info("🔄 MISE À JOUR STATUT - Commande {}: {} -> {}", order.getId(), oldStatus, newStatus);

        if (shouldUpdateOrderStatus(oldStatus, newStatus)) {
            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());

            if (newStatus == OrderStatus.CONFIRMED) {
                order.setPaidAt(LocalDateTime.now());
                order.setPaymentStatus("succeeded");
                order.setCheckoutToken(null);

                // 🆕 AUTO-VERIFY: Mark user's email as verified upon confirmed payment
                if (order.getUser() != null) {
                    autoVerifyUserOnPayment(order.getUser());
                }
            }
            OrderProgressSync.applyMinimumForStatus(order);

            orderRepository.save(order);

            logger.info("✅ STATUT MISE À JOUR - Commande {} mis à jour: {} -> {}",
                    order.getId(), oldStatus, newStatus);

            orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, oldStatus, newStatus);

            // 🆕 Envoi automatique de la facture par email après confirmation du paiement
            if (newStatus == OrderStatus.CONFIRMED && order.getUser() != null) {
                sendInvoiceByEmail(order, order.getUser());
            }
        } else {
            logger.warn("❌ TRANSITION INVALIDE - Mise à jour de statut bloquée pour commande {}: {} -> {}",
                    order.getId(), oldStatus, newStatus);
        }
    }

    /**
     * 🆕 NOUVEAUTÉ : Auto-vérifie l'email de l'utilisateur après un paiement confirmé.
     * Si un utilisateur paie, on peut considérer que son email est valide.
     */
    private void autoVerifyUserOnPayment(User user) {
        try {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                userRepository.save(user);
                logger.info("✅ AUTO-VERIFY - Email vérifié automatiquement pour l'utilisateur {} suite au paiement",
                        user.getEmail());
                securityLogger.info("User email auto-verified on payment - User: {}", user.getEmail());
            }
        } catch (Exception e) {
            logger.error("❌ AUTO-VERIFY - Erreur lors de la vérification automatique pour {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    /**
     * 🆕 NOUVEAUTÉ : Envoie la facture PDF par email au client après un paiement
     * réussi
     */
    private void sendInvoiceByEmail(Order order, User user) {
        try {
            logger.info("📧 ENVOI FACTURE - Génération et envoi de la facture pour la commande {} à {}",
                    order.getId(), user.getEmail());

            // Générer le PDF de la facture
            byte[] pdfData = invoicePdfService.generateInvoicePdf(order, user);

            if (pdfData == null || pdfData.length == 0) {
                logger.error("❌ ENVOI FACTURE - Échec de génération du PDF pour la commande {}", order.getId());
                return;
            }

            // Générer le numéro de facture pour le nom du fichier
            String invoiceNumber = invoicePdfService.generateInvoiceNumber(order);
            String fileName = "Facture-" + invoiceNumber + ".pdf";

            // Préparer le contenu de l'email
            String subject = "Votre facture LMP - " + invoiceNumber;
            String body = buildInvoiceEmailBody(order, user, invoiceNumber);

            // Envoyer l'email avec la facture en pièce jointe
            emailService.sendEmailWithAttachment(
                    user.getEmail(),
                    subject,
                    body,
                    fileName,
                    pdfData,
                    "application/pdf");

            logger.info("✅ ENVOI FACTURE - Facture {} envoyée avec succès à {}",
                    invoiceNumber, user.getEmail());

            securityLogger.info("Invoice sent via email - Order: {}, Invoice: {}, Email: {}",
                    order.getId(), invoiceNumber, user.getEmail());

            Map<String, Object> inv = new HashMap<>();
            inv.put("orderId", order.getId().toString());
            inv.put("userId", user.getId().toString());
            inv.put("serviceName", order.getServiceName());
            inv.put("invoiceNumber", invoiceNumber);
            inv.put(BusinessEventPayloadKeys.MESSAGE, String.format("Facture disponible — commande %s",
                    order.getId()));
            inv.put(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE, "Votre facture est prête");
            inv.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.TRUE);
            inv.put(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE, "INVOICE_READY");
            inv.put(BusinessEventPayloadKeys.SSE_DASHBOARD_PING, Boolean.FALSE);
            eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.INVOICE_GENERATED, "billing", order.getId(), inv));

        } catch (Exception e) {
            logger.error("❌ ENVOI FACTURE - Erreur lors de l'envoi de la facture pour la commande {}: {}",
                    order.getId(), e.getMessage(), e);
            // Ne pas propager l'exception pour ne pas bloquer le traitement du webhook
        }
    }

    /**
     * 🆕 AMÉLIORÉ : Construit le corps HTML de l'email de facture via template Thymeleaf
     */
    private String buildInvoiceEmailBody(Order order, User user, String invoiceNumber) {
        java.time.format.DateTimeFormatter emailDateFormatter = java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy à HH:mm");

        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) {
            fullName = user.getEmail();
        }

        String formattedDate = order.getPaidAt() != null
                ? order.getPaidAt().format(emailDateFormatter)
                : "N/A";

        try {
            Context context = new Context();
            context.setVariable("customerName", fullName);
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("serviceName", order.getServiceName());
            context.setVariable("amount", order.getTotalAmount());
            context.setVariable("currency", order.getCurrency() != null ? order.getCurrency() : "EUR");
            context.setVariable("paymentDate", formattedDate);

            return templateEngine.process("emails/invoice-receipt", context);
        } catch (Exception e) {
            logger.warn("Échec du rendu template HTML facture, fallback texte brut: {}", e.getMessage());
            // Fallback texte brut
            return "Bonjour " + fullName + ",\n\n"
                    + "Merci pour votre achat chez LMP !\n\n"
                    + "Veuillez trouver ci-joint votre facture n° " + invoiceNumber + ".\n\n"
                    + "- Service : " + order.getServiceName() + "\n"
                    + "- Montant : " + order.getTotalAmount() + " " + order.getCurrency() + "\n"
                    + "- Date de paiement : " + formattedDate + "\n\n"
                    + "Cordialement,\nL'équipe LMP\n";
        }
    }

    private void publishPaymentFailed(PaymentIntent paymentIntent) {
        Map<String, Object> pl = new HashMap<>();
        pl.put("paymentIntentId", paymentIntent.getId());
        String err = paymentIntent.getLastPaymentError() != null
                ? paymentIntent.getLastPaymentError().toString()
                : "failed";
        pl.put("error", err);

        Optional<Order> orderOpt = orderRepository.findByStripePaymentIntentId(paymentIntent.getId());
        if (orderOpt.isEmpty() && paymentIntent.getMetadata() != null
                && paymentIntent.getMetadata().get("session_id") != null) {
            orderOpt = orderRepository.findByStripeSessionId(paymentIntent.getMetadata().get("session_id"));
        }

        UUID entityId;
        if (orderOpt.isPresent()) {
            Order o = orderOpt.get();
            entityId = o.getId();
            pl.put("orderId", o.getId().toString());
            if (o.getUser() != null) {
                pl.put("userId", o.getUser().getId().toString());
            }
        } else {
            entityId = UUID.randomUUID();
        }
        String adminLine = String.format("Échec de paiement — %s", err);
        pl.put(BusinessEventPayloadKeys.MESSAGE, adminLine);
        if (orderOpt.isPresent() && orderOpt.get().getUser() != null) {
            pl.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.TRUE);
            pl.put(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE, "PAYMENT_ERROR");
            pl.put(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE, adminLine);
        }
        eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.PAYMENT_FAILED, "billing", entityId, pl));
    }
}
