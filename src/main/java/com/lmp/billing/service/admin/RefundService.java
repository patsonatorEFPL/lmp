package com.lmp.billing.service.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.domain.Refund;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.RefundRepository;
import com.lmp.billing.dto.admin.RefundDto;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.notification.service.NotificationService;
import com.lmp.shared.util.AuthenticatedActor;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;

/**
 * Service pour la gestion des remboursements avec intégration Stripe.
 * Gère les remboursements partiels et complets.
 */
@Service
@Transactional
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

        private final RefundRepository refundRepository;

        private final OrderRepository orderRepository;

        private final NotificationService notificationService;

        private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;

        private final StripeClient stripeClient;


    public RefundService(RefundRepository refundRepository,
                           OrderRepository orderRepository,
                           NotificationService notificationService,
                           OrderRealtimeEventPublisher orderRealtimeEventPublisher,
                           StripeClient stripeClient) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
        this.stripeClient = stripeClient;
    }

    /**
     * Crée un remboursement complet
     */
    public RefundDto createRefund(java.util.UUID orderId, BigDecimal amount, String reason) throws StripeException {
        logger.info("Création remboursement pour commande {} - montant: {}", orderId, amount);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        // Validation du remboursement
        validateRefundRequest(order, amount);

        // Calcul du montant en centimes pour Stripe
        Long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // Création du remboursement Stripe
        com.stripe.model.Refund stripeRefund = createStripeRefund(
            order.getStripePaymentIntentId(), amountCents, reason);

        // Création de l'entité Refund
        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setAmount(amount);
        refund.setCurrency(order.getCurrency());
        refund.setReason(reason);
        refund.setStripeRefundId(stripeRefund.getId());
        refund.setStatus(stripeRefund.getStatus());
        refund.setCreatedAt(LocalDateTime.now());
        refund.setProcessedAt(LocalDateTime.now());
        refund.setProcessedBy(AuthenticatedActor.nameOrSystem());

        refund = refundRepository.save(refund);

        // Mise à jour du statut de la commande si remboursement complet
        OrderStatus statusBeforeRefund = order.getStatus();
        if (amount.compareTo(order.getTotalAmount()) >= 0) {
            order.setStatus(OrderStatus.REFUNDED);
            OrderProgressSync.applyMinimumForStatus(order);
            orderRepository.save(order);
            orderRealtimeEventPublisher.publishOrderUpdated(order, statusBeforeRefund, OrderStatus.REFUNDED);
        }

        // Notification client
        notificationService.sendRefundNotification(order, 
            amount.toString() + " " + order.getCurrency(), 
            stripeRefund.getId());

        logger.info("Remboursement créé avec succès: {}", refund.getId());
        return convertToDto(refund);
    }

    /**
     * Récupère tous les remboursements d'une commande
     */
    @Transactional(readOnly = true)
    public List<RefundDto> getOrderRefunds(java.util.UUID orderId) {
        List<Refund> refunds = refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return refunds.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Calcule le montant total remboursé pour une commande
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRefundedAmount(java.util.UUID orderId) {
        return refundRepository.getTotalRefundedAmountByOrderId(orderId);
    }

    // ========== Méthodes privées ==========

    private void validateRefundRequest(Order order, BigDecimal amount) {
        if (order.getStripePaymentIntentId() == null) {
            throw new IllegalStateException("Aucun PaymentIntent Stripe associé à cette commande");
        }

        if (order.getStatus() == OrderStatus.PENDING || 
            order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Impossible de rembourser une commande non payée");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du remboursement doit être positif");
        }

        BigDecimal totalRefunded = getTotalRefundedAmount(order.getId());
        BigDecimal maxRefundable = order.getTotalAmount().subtract(totalRefunded);
        
        if (amount.compareTo(maxRefundable) > 0) {
            throw new IllegalArgumentException("Le montant du remboursement dépasse le montant disponible");
        }
    }

    private com.stripe.model.Refund createStripeRefund(String paymentIntentId, Long amountCents, String reason) 
            throws StripeException {

        com.stripe.param.RefundCreateParams params = com.stripe.param.RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amountCents)
                .setReason(com.stripe.param.RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .putMetadata("refund_reason", reason)
                .putMetadata("refund_created_at", LocalDateTime.now().toString())
                .putMetadata("refund_source", "admin_panel")
                .build();
        return stripeClient.refunds().create(params);
    }

    private RefundDto convertToDto(Refund refund) {
        RefundDto dto = new RefundDto();
        
        dto.setId(refund.getId());
        dto.setOrderId(refund.getOrder().getId());
        dto.setAmount(refund.getAmount());
        dto.setCurrency(refund.getCurrency());
        dto.setReason(refund.getReason());
        dto.setStatus(refund.getStatus());
        dto.setStripeRefundId(refund.getStripeRefundId());
        dto.setFailureReason(refund.getFailureReason());
        dto.setCreatedAt(refund.getCreatedAt());
        dto.setProcessedAt(refund.getProcessedAt());
        dto.setProcessedBy(refund.getProcessedBy());
        dto.setCancellationReason(refund.getCancellationReason());
        dto.setCancelledAt(refund.getCancelledAt());
        dto.setCancelledBy(refund.getCancelledBy());
        dto.setMetadata(refund.getMetadata());
        
        // Informations de la commande
        if (refund.getOrder() != null) {
            dto.setOrderServiceName(refund.getOrder().getServiceName());
            if (refund.getOrder().getUser() != null) {
                dto.setCustomerEmail(refund.getOrder().getUser().getEmail());
                dto.setCustomerName(refund.getOrder().getUser().getFirstName() + " " + 
                    refund.getOrder().getUser().getLastName());
            }
        }
        
        return dto;
    }
}