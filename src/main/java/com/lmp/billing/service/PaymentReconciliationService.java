package com.lmp.billing.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.notification.service.EmailService;
import com.lmp.billing.service.InvoicePdfService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

/**
 * Service de réconciliation des paiements.
 * Vérifie périodiquement les commandes bloquées en PAYMENT_PENDING
 * auprès de l'API Stripe et met à jour leur statut en conséquence.
 * Récupère également les commandes annulées à tort si le paiement a bien été effectué.
 */
@Service
@Transactional
public class PaymentReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReconciliationService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + PaymentReconciliationService.class.getName());

        private final OrderRepository orderRepository;

        private final InvoicePdfService invoicePdfService;

        private final EmailService emailService;

        private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    /**
     * Délai minimum (en minutes) avant de réconcilier une commande PAYMENT_PENDING.
     * Laisse le temps au webhook Stripe d'arriver naturellement.
     */
    @Value("${lmp.reconciliation.pending.delay.minutes:2}")
    private int pendingDelayMinutes;

    /**
     * Période (en heures) pour vérifier les commandes CANCELLED récentes.
     * Récupère les commandes annulées à tort par le cleanup si Stripe confirme le paiement.
     */
    @Value("${lmp.reconciliation.cancelled.lookback.hours:24}")
    private int cancelledLookbackHours;


    public PaymentReconciliationService(OrderRepository orderRepository,
                           InvoicePdfService invoicePdfService,
                           EmailService emailService,
                           OrderRealtimeEventPublisher orderRealtimeEventPublisher) {
        this.orderRepository = orderRepository;
        this.invoicePdfService = invoicePdfService;
        this.emailService = emailService;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
    }

    /**
     * Réconciliation des commandes PAYMENT_PENDING avec Stripe.
     * Exécutée toutes les 5 minutes (configurable).
     */
    @Scheduled(fixedDelayString = "${lmp.reconciliation.interval.ms:300000}")
    public void reconcileStaleOrders() {
        logger.info("🔄 RÉCONCILIATION - Début de la réconciliation des paiements");

        int reconciledCount = 0;
        int recoveredCount = 0;
        int errorCount = 0;

        try {
            Stripe.apiKey = stripeSecretKey;

            // 1. Réconcilier les commandes PAYMENT_PENDING
            reconciledCount = reconcilePaymentPendingOrders();

            // 2. Récupérer les commandes CANCELLED qui ont été payées
            recoveredCount = recoverCancelledPaidOrders();

        } catch (Exception e) {
            logger.error("❌ RÉCONCILIATION - Erreur critique : {}", e.getMessage(), e);
            errorCount++;
        }

        logger.info("✅ RÉCONCILIATION - Terminée : {} réconciliées, {} récupérées, {} erreurs",
                reconciledCount, recoveredCount, errorCount);
    }

    /**
     * Vérifie les commandes PAYMENT_PENDING auprès de Stripe et met à jour leur statut.
     */
    private int reconcilePaymentPendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingDelayMinutes);
        List<Order> staleOrders = orderRepository.findStaleOrdersWithStripeSession(
                OrderStatus.PAYMENT_PENDING, cutoff);
        List<Order> piOnly = orderRepository.findStaleOrdersWithPaymentIntentOnly(
                OrderStatus.PAYMENT_PENDING, cutoff);

        if (staleOrders.isEmpty() && piOnly.isEmpty()) {
            logger.debug("RÉCONCILIATION - Aucune commande PAYMENT_PENDING à vérifier");
            return 0;
        }

        logger.info("🔍 RÉCONCILIATION - {} commandes (session) + {} (PaymentIntent seul) PAYMENT_PENDING à vérifier",
                staleOrders.size(), piOnly.size());

        int reconciledCount = 0;

        for (Order order : staleOrders) {
            try {
                boolean reconciled = reconcileOrderWithStripe(order);
                if (reconciled) {
                    reconciledCount++;
                }
            } catch (Exception e) {
                logger.error("❌ RÉCONCILIATION - Erreur pour la commande {} : {}",
                        order.getId(), e.getMessage());
            }
        }

        for (Order order : piOnly) {
            try {
                if (reconcileOrderWithStripe(order)) {
                    reconciledCount++;
                }
            } catch (Exception e) {
                logger.error("❌ RÉCONCILIATION - Erreur pour la commande {} : {}",
                        order.getId(), e.getMessage());
            }
        }

        return reconciledCount;
    }

    /**
     * Vérifie les commandes CANCELLED récentes pour récupérer celles qui ont été payées.
     */
    private int recoverCancelledPaidOrders() {
        LocalDateTime since = LocalDateTime.now().minusHours(cancelledLookbackHours);
        List<Order> cancelledOrders = orderRepository.findRecentlyCancelledWithStripeSession(since);

        if (cancelledOrders.isEmpty()) {
            logger.debug("RÉCONCILIATION - Aucune commande CANCELLED récente à vérifier");
            return 0;
        }

        logger.info("🔍 RÉCONCILIATION - {} commandes CANCELLED récentes à vérifier auprès de Stripe",
                cancelledOrders.size());

        int recoveredCount = 0;

        for (Order order : cancelledOrders) {
            try {
                boolean recovered = reconcileOrderWithStripe(order);
                if (recovered) {
                    recoveredCount++;
                    auditLogger.info("🔄 RÉCUPÉRATION - Commande {} récupérée de CANCELLED à CONFIRMED via réconciliation",
                            order.getId());
                }
            } catch (Exception e) {
                logger.error("❌ RÉCONCILIATION - Erreur de récupération pour la commande {} : {}",
                        order.getId(), e.getMessage());
            }
        }

        return recoveredCount;
    }

    /**
     * Interroge Stripe immédiatement pour la session Checkout de la commande (sans attendre le job planifié).
     * Utile lorsque le webhook tarde ou n'est pas disponible (ex. localhost sans Stripe CLI).
     *
     * @return {@code true} si un changement a été persisté (confirmation, annulation session expirée, etc.)
     */
    public boolean syncCheckoutSessionImmediately(Order order) {
        return reconcileOrderWithStripe(order);
    }

    /**
     * Session Checkout si présente, sinon PaymentIntent (Payment Element).
     */
    public boolean syncOrderPaymentImmediately(Order order) {
        return reconcileOrderWithStripe(order);
    }

    /**
     * Vérifie une commande individuelle auprès de Stripe et met à jour son statut.
     *
     * @return true si la commande a été mise à jour, false sinon
     */
    private boolean reconcileOrderWithStripe(Order order) {
        String sessionId = order.getStripeSessionId();
        if (sessionId != null && !sessionId.isEmpty()) {
            return reconcileCheckoutSession(order, sessionId);
        }

        String piId = order.getStripePaymentIntentId();
        if (piId != null && !piId.isEmpty()) {
            return reconcilePaymentIntent(order, piId);
        }

        return false;
    }

    private boolean reconcileCheckoutSession(Order order, String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            String paymentStatus = session.getPaymentStatus();

            logger.info("🔍 RÉCONCILIATION - Commande {} (status={}), Stripe session {} → payment_status='{}'",
                    order.getId(), order.getStatus(), sessionId, paymentStatus);

            if ("paid".equals(paymentStatus)) {
                return confirmOrder(order, session);
            } else if ("unpaid".equals(paymentStatus)) {
                // Vérifier si la session a expiré
                if (session.getExpiresAt() != null
                        && session.getExpiresAt() < java.time.Instant.now().getEpochSecond()) {
                    // Session expirée et non payée → annuler seulement si encore PAYMENT_PENDING
                    if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                        OrderStatus previous = order.getStatus();
                        order.setStatus(OrderStatus.CANCELLED);
                        order.setCancellationReason("Session Stripe expirée sans paiement (réconciliation)");
                        order.setCancelledAt(LocalDateTime.now());
                        order.setUpdatedAt(LocalDateTime.now());
                        OrderProgressSync.applyMinimumForStatus(order);
                        orderRepository.save(order);
                        orderRealtimeEventPublisher.publishOrderUpdated(order, previous, OrderStatus.CANCELLED);

                        logger.info("⏰ RÉCONCILIATION - Commande {} annulée : session Stripe expirée",
                                order.getId());
                        return true;
                    }
                }
            }

            return false;

        } catch (com.stripe.exception.InvalidRequestException e) {
            // Session introuvable chez Stripe (supprimée, invalide, etc.)
            logger.warn("⚠️ RÉCONCILIATION - Session Stripe introuvable pour commande {} : {}",
                    order.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("❌ RÉCONCILIATION - Erreur Stripe pour commande {} : {}",
                    order.getId(), e.getMessage());
            return false;
        }
    }

    private boolean reconcilePaymentIntent(Order order, String paymentIntentId) {
        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            String status = pi.getStatus();

            logger.info("🔍 RÉCONCILIATION - Commande {} (status={}), Stripe PI {} → status='{}'",
                    order.getId(), order.getStatus(), paymentIntentId, status);

            if ("succeeded".equals(status)) {
                return confirmOrderFromPaymentIntent(order, pi);
            }
            if ("canceled".equals(status) && order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                order.setPaymentStatus("cancelled");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                return true;
            }

            return false;
        } catch (com.stripe.exception.InvalidRequestException e) {
            logger.warn("⚠️ RÉCONCILIATION - PaymentIntent Stripe introuvable pour commande {} : {}",
                    order.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("❌ RÉCONCILIATION - Erreur PaymentIntent pour commande {} : {}",
                    order.getId(), e.getMessage());
            return false;
        }
    }

    private boolean confirmOrderFromPaymentIntent(Order order, PaymentIntent pi) {
        OrderStatus previousStatus = order.getStatus();

        if (previousStatus == OrderStatus.CONFIRMED
                || previousStatus == OrderStatus.PROCESSING
                || previousStatus == OrderStatus.IN_PROGRESS
                || previousStatus == OrderStatus.COMPLETED
                || previousStatus == OrderStatus.SHIPPED
                || previousStatus == OrderStatus.DELIVERED) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus("succeeded");
        order.setPaidAt(now);
        order.setUpdatedAt(now);
        order.setStripePaymentIntentId(pi.getId());
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isBlank()) {
            order.setPaymentMethod("payment_element");
        }

        OrderProgressSync.applyMinimumForStatus(order);

        if (previousStatus == OrderStatus.CANCELLED) {
            order.setCancellationReason(null);
            order.setCancelledAt(null);
        }

        orderRepository.save(order);

        orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, previousStatus, OrderStatus.CONFIRMED);

        logger.info("✅ RÉCONCILIATION - Commande {} confirmée ({} → CONFIRMED) via PaymentIntent",
                order.getId(), previousStatus);

        auditLogger.info("RECONCILIATION - Order {} status updated: {} → CONFIRMED, PaymentIntent: {}",
                order.getId(), previousStatus, pi.getId());

        if (order.getUser() != null) {
            sendInvoiceAsync(order);
        }

        return true;
    }

    /**
     * Confirme une commande dont le paiement a été vérifié auprès de Stripe.
     */
    private boolean confirmOrder(Order order, Session session) {
        OrderStatus previousStatus = order.getStatus();

        // Ne pas re-confirmer une commande déjà confirmée ou plus avancée
        if (previousStatus == OrderStatus.CONFIRMED
                || previousStatus == OrderStatus.PROCESSING
                || previousStatus == OrderStatus.IN_PROGRESS
                || previousStatus == OrderStatus.COMPLETED
                || previousStatus == OrderStatus.SHIPPED
                || previousStatus == OrderStatus.DELIVERED) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus("succeeded");
        order.setPaidAt(now);
        order.setUpdatedAt(now);

        // Enrichir avec les données Stripe si manquantes
        if (order.getStripePaymentIntentId() == null && session.getPaymentIntent() != null) {
            order.setStripePaymentIntentId(session.getPaymentIntent());
        }
        if (order.getStripeCustomerId() == null && session.getCustomer() != null) {
            order.setStripeCustomerId(session.getCustomer());
        }
        if (order.getPaymentMethod() == null) {
            order.setPaymentMethod("stripe_checkout");
        }

        OrderProgressSync.applyMinimumForStatus(order);

        // Si la commande était annulée, nettoyer la raison d'annulation
        if (previousStatus == OrderStatus.CANCELLED) {
            order.setCancellationReason(null);
            order.setCancelledAt(null);
        }

        orderRepository.save(order);

        orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, previousStatus, OrderStatus.CONFIRMED);

        logger.info("✅ RÉCONCILIATION - Commande {} confirmée ({} → CONFIRMED) via vérification Stripe",
                order.getId(), previousStatus);

        auditLogger.info("RECONCILIATION - Order {} status updated: {} → CONFIRMED, Stripe session: {}",
                order.getId(), previousStatus, session.getId());

        // Envoyer la facture si un utilisateur est associé
        if (order.getUser() != null) {
            sendInvoiceAsync(order);
        }

        return true;
    }

    /**
     * Envoie la facture par email de manière non-bloquante.
     */
    private void sendInvoiceAsync(Order order) {
        try {
            byte[] pdfData = invoicePdfService.generateInvoicePdf(order, order.getUser());
            if (pdfData == null || pdfData.length == 0) {
                logger.warn("⚠️ RÉCONCILIATION - Échec de génération du PDF pour la commande {}", order.getId());
                return;
            }

            String invoiceNumber = invoicePdfService.generateInvoiceNumber(order);
            String fileName = "Facture-" + invoiceNumber + ".pdf";
            String subject = "Votre facture LMP - " + invoiceNumber;

            String body = buildReconciliationInvoiceBody(order, invoiceNumber);

            emailService.sendEmailWithAttachment(
                    order.getUser().getEmail(),
                    subject,
                    body,
                    fileName,
                    pdfData,
                    "application/pdf");

            logger.info("📧 RÉCONCILIATION - Facture {} envoyée à {}", invoiceNumber, order.getUser().getEmail());

        } catch (Exception e) {
            logger.error("❌ RÉCONCILIATION - Erreur d'envoi de facture pour commande {} : {}",
                    order.getId(), e.getMessage());
        }
    }

    /**
     * Construit le corps de l'email de facture pour la réconciliation.
     */
    private String buildReconciliationInvoiceBody(Order order, String invoiceNumber) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        String firstName = order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "";
        String lastName = order.getUser().getLastName() != null ? order.getUser().getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) {
            fullName = order.getUser().getEmail();
        }

        String formattedDate = order.getPaidAt() != null ? order.getPaidAt().format(fmt) : "N/A";

        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(fullName).append(",\n\n");
        body.append("Merci pour votre achat chez LMP !\n\n");
        body.append("Veuillez trouver ci-joint votre facture n° ").append(invoiceNumber).append(".\n\n");
        body.append("Détails de votre commande :\n");
        body.append("- Service : ").append(order.getServiceName()).append("\n");
        body.append("- Montant : ").append(order.getTotalAmount()).append(" ").append(order.getCurrency()).append("\n");
        body.append("- Date de paiement : ").append(formattedDate).append("\n\n");
        body.append("Si vous avez des questions, n'hésitez pas à nous contacter.\n\n");
        body.append("Cordialement,\n");
        body.append("L'équipe LMP\n");

        return body.toString();
    }
}
