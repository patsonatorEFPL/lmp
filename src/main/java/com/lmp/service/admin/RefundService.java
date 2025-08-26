package com.lmp.service.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.Refund;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.RefundRepository;
import com.lmp.web.dto.admin.RefundDto;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

/**
 * Service pour la gestion des remboursements avec intégration Stripe.
 * Gère les remboursements partiels et complets.
 */
@Service
@Transactional
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Crée un remboursement complet
     */
    public RefundDto createRefund(Long orderId, BigDecimal amount, String reason) throws StripeException {
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
        refund.setProcessedBy("ADMIN"); // TODO: Récupérer l'utilisateur connecté

        refund = refundRepository.save(refund);

        // Mise à jour du statut de la commande si remboursement complet
        if (amount.compareTo(order.getTotalAmount()) >= 0) {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
        }

        // Notification client
        notificationService.sendRefundNotification(order, 
            amount.toString() + " " + order.getCurrency(), 
            stripeRefund.getId());

        logger.info("Remboursement créé avec succès: {}", refund.getId());
        return convertToDto(refund);
    }

    /**
     * Crée un remboursement partiel
     */
    public RefundDto createPartialRefund(Long orderId, BigDecimal amount, String reason) throws StripeException {
        logger.info("Création remboursement partiel pour commande {} - montant: {}", orderId, amount);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        // Validation du remboursement partiel
        validatePartialRefundRequest(order, amount);

        return createRefund(orderId, amount, reason);
    }

    /**
     * Récupère tous les remboursements d'une commande
     */
    @Transactional(readOnly = true)
    public List<RefundDto> getOrderRefunds(Long orderId) {
        List<Refund> refunds = refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return refunds.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Récupère un remboursement par ID
     */
    @Transactional(readOnly = true)
    public RefundDto getRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new RuntimeException("Remboursement non trouvé: " + refundId));
        return convertToDto(refund);
    }

    /**
     * Recherche avancée des remboursements
     */
    @Transactional(readOnly = true)
    public Page<RefundDto> searchRefunds(String customerEmail, String status, 
                                       LocalDateTime startDate, LocalDateTime endDate,
                                       BigDecimal minAmount, BigDecimal maxAmount,
                                       int page, int size, String sortBy, String sortDir) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Refund> refunds = refundRepository.searchRefundsAdvanced(
            customerEmail, status, startDate, endDate, minAmount, maxAmount, pageable);

        return refunds.map(this::convertToDto);
    }

    /**
     * Synchronise un remboursement avec Stripe
     */
    public RefundDto syncWithStripe(Long refundId) throws StripeException {
        logger.info("Synchronisation Stripe pour remboursement {}", refundId);

        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new RuntimeException("Remboursement non trouvé: " + refundId));

        if (refund.getStripeRefundId() == null) {
            throw new IllegalStateException("Aucun ID Stripe associé au remboursement");
        }

        // Récupération des informations Stripe
        com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.retrieve(refund.getStripeRefundId());
        
        // Mise à jour du statut
        String oldStatus = refund.getStatus();
        refund.setStatus(stripeRefund.getStatus());
        refund.setFailureReason(stripeRefund.getFailureReason());
        
        // Mise à jour des métadonnées Stripe si disponibles
        if (stripeRefund.getMetadata() != null) {
            refund.setMetadata(stripeRefund.getMetadata().toString());
        }

        refund = refundRepository.save(refund);
        
        logger.info("Synchronisation Stripe réussie: {} -> {}", oldStatus, refund.getStatus());
        return convertToDto(refund);
    }

    /**
     * Annule un remboursement en attente
     */
    public RefundDto cancelRefund(Long refundId, String reason) {
        logger.info("Annulation remboursement {}", refundId);

        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new RuntimeException("Remboursement non trouvé: " + refundId));

        if (!"pending".equals(refund.getStatus())) {
            throw new IllegalStateException("Seuls les remboursements en attente peuvent être annulés");
        }

        refund.setStatus("cancelled");
        refund.setCancellationReason(reason);
        refund.setCancelledAt(LocalDateTime.now());
        refund.setCancelledBy("ADMIN"); // TODO: Récupérer l'utilisateur connecté

        refund = refundRepository.save(refund);
        
        logger.info("Remboursement {} annulé", refundId);
        return convertToDto(refund);
    }

    /**
     * Statistiques des remboursements
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRefundStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // Statistiques globales
        Object[] globalStats = refundRepository.getRefundStatsSince(startDate);
        stats.put("totalRefunds", globalStats[0]);
        stats.put("totalRefundAmount", globalStats[1]);
        stats.put("averageRefundAmount", globalStats[2]);
        
        // Statistiques par statut
        List<Object[]> statusStats = refundRepository.getRefundStatsByStatus();
        stats.put("refundsByStatus", statusStats);
        
        // Statistiques mensuelles
        List<Object[]> monthlyStats = refundRepository.getMonthlyRefundStats(startDate);
        stats.put("monthlyRefunds", monthlyStats);
        
        // Top des raisons de remboursement
        List<Object[]> topReasons = refundRepository.getTopRefundReasons(
            startDate, PageRequest.of(0, 10));
        stats.put("topRefundReasons", topReasons);
        
        return stats;
    }

    /**
     * Calcule le montant total remboursé pour une commande
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRefundedAmount(Long orderId) {
        return refundRepository.getTotalRefundedByOrder(orderId);
    }

    /**
     * Vérifie si une commande peut être remboursée
     */
    @Transactional(readOnly = true)
    public boolean canBeRefunded(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));
        
        // Vérifications de base
        if (order.getStripePaymentIntentId() == null) {
            return false;
        }
        
        if (order.getStatus() == OrderStatus.PENDING || 
            order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            return false;
        }
        
        // Vérifier qu'il reste du montant à rembourser
        BigDecimal totalRefunded = getTotalRefundedAmount(orderId);
        return order.getTotalAmount().compareTo(totalRefunded) > 0;
    }

    /**
     * Calcule le montant maximum remboursable
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxRefundableAmount(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));
        
        BigDecimal totalRefunded = getTotalRefundedAmount(orderId);
        BigDecimal maxRefundable = order.getTotalAmount().subtract(totalRefunded);
        
        return maxRefundable.max(BigDecimal.ZERO);
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

    private void validatePartialRefundRequest(Order order, BigDecimal amount) {
        validateRefundRequest(order, amount);
        
        if (amount.compareTo(order.getTotalAmount()) >= 0) {
            throw new IllegalArgumentException("Utilisez createRefund() pour un remboursement complet");
        }
    }

    private com.stripe.model.Refund createStripeRefund(String paymentIntentId, Long amountCents, String reason) 
            throws StripeException {
        
        Map<String, Object> refundParams = new HashMap<>();
        refundParams.put("payment_intent", paymentIntentId);
        refundParams.put("amount", amountCents);
        refundParams.put("reason", "requested_by_customer");
        
        // Métadonnées pour traçabilité
        Map<String, String> metadata = new HashMap<>();
        metadata.put("refund_reason", reason);
        metadata.put("refund_created_at", LocalDateTime.now().toString());
        metadata.put("refund_source", "admin_panel");
        refundParams.put("metadata", metadata);

        return com.stripe.model.Refund.create(refundParams);
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