package com.lmp.billing.service.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.billing.domain.Order;
import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.dto.admin.OrderDto;
import com.lmp.billing.dto.admin.OrderSearchDto;
import com.lmp.billing.dto.admin.OrderActionDto;
import com.lmp.billing.dto.admin.OrderReportDto;
import com.lmp.notification.service.NotificationService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

/**
 * Service d'administration pour la gestion complète des commandes.
 * Coordonne les opérations CRUD, recherche avancée, statistiques et actions admin.
 */
@Service
@Transactional
public class OrderAdminService {

    private static final Logger logger = LoggerFactory.getLogger(OrderAdminService.class);

        private final OrderRepository orderRepository;

        private final UserRepository userRepository;

        private final OrderStatusHistoryService orderStatusHistoryService;

        private final NotificationService notificationService;

        private final RefundService refundService;

        private final ReportsService reportsService;

        private final ApplicationEventPublisher eventPublisher;

        private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;


    public OrderAdminService(OrderRepository orderRepository,
                           UserRepository userRepository,
                           OrderStatusHistoryService orderStatusHistoryService,
                           NotificationService notificationService,
                           RefundService refundService,
                           ReportsService reportsService,
                           ApplicationEventPublisher eventPublisher,
                           OrderRealtimeEventPublisher orderRealtimeEventPublisher) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.notificationService = notificationService;
        this.refundService = refundService;
        this.reportsService = reportsService;
        this.eventPublisher = eventPublisher;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
    }

    // ========== CRUD et Recherche ==========

    /**
     * Recherche avancée avec tous les filtres
     */
    @Transactional(readOnly = true)
    public Page<OrderDto> searchOrders(OrderSearchDto searchDto, int page, int size, String sortBy, String sortDir) {
        logger.info("Recherche avancée commandes avec critères: {}", searchDto);

        // Configuration pagination et tri
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Conversion du statut String vers OrderStatus
        OrderStatus statusEnum = null;
        if (searchDto.getStatus() != null && !searchDto.getStatus().trim().isEmpty()) {
            try {
                statusEnum = OrderStatus.valueOf(searchDto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Si le statut n'est pas valide, on l'ignore
                logger.warn("Statut invalide ignoré: {}", searchDto.getStatus());
            }
        }
        
        // Recherche avec tous les critères
        Page<Order> orders = orderRepository.searchOrdersAdvanced(
            searchDto.getSearchTerm(),
            searchDto.getCustomerEmail(),
            searchDto.getServiceName(),
            statusEnum,
            searchDto.getMinAmount(),
            searchDto.getMaxAmount(),
            searchDto.getStartDate(),
            searchDto.getEndDate(),
            searchDto.getPaymentStatus(),
            searchDto.getHasRefunds(),
            pageable
        );

        return orders.map(this::convertToDto);
    }

    /**
     * Récupère les détails complets d'une commande
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderDetails(java.util.UUID orderId) {
        logger.info("Récupération détails commande ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        OrderDto dto = convertToDto(order);
        
        // Enrichir avec l'historique des statuts
        dto.setStatusHistory(orderStatusHistoryService.getOrderHistory(orderId));
        
        // Enrichir avec les informations de remboursement
        dto.setRefunds(refundService.getOrderRefunds(orderId));

        return dto;
    }

    /**
     * Tableau de bord rapide - dernières commandes
     */
    @Transactional(readOnly = true)
    public Page<OrderDto> getLatestOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findLatestOrders(pageable);
        return orders.map(this::convertToDto);
    }

    /**
     * Commandes nécessitant une attention
     */
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersNeedingAttention() {
        LocalDateTime oldPendingCutoff = LocalDateTime.now().minusHours(24);
        LocalDateTime paymentPendingCutoff = LocalDateTime.now().minusHours(2);
        
        List<Order> orders = orderRepository.findOrdersNeedingAttention(
            oldPendingCutoff, paymentPendingCutoff);
        
        return orders.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    // ========== Actions administratives ==========

    /**
     * Change le statut d'une commande avec validation et historique
     */
    public OrderDto changeOrderStatus(java.util.UUID orderId, OrderStatus newStatus, String adminNote) {
        logger.info("Changement statut commande {} vers {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        
        // Validation de la transition de statut
        validateStatusTransition(oldStatus, newStatus);

        // Mise à jour des dates spécifiques selon le statut
        updateStatusSpecificFields(order, newStatus);

        // Sauvegarde du changement
        order.setStatus(newStatus);
        order.setLastModifiedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // Enregistrement dans l'historique
        orderStatusHistoryService.recordStatusChange(order, oldStatus, newStatus, adminNote);

        // Notification automatique du client
        notificationService.sendOrderStatusNotification(order, oldStatus, newStatus);

        OrderDto orderDto = convertToDto(order);
        publishOrderUpdated(order, orderDto, oldStatus.toString(), newStatus.toString());

        logger.info("Statut commande {} changé: {} -> {}", orderId, oldStatus, newStatus);
        return orderDto;
    }

    /**
     * Annule une commande avec gestion des remboursements
     */
    public OrderDto cancelOrder(java.util.UUID orderId, String reason, boolean processRefund) {
        logger.info("Annulation commande {} avec remboursement: {}", orderId, processRefund);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        // Vérification que la commande peut être annulée
        if (order.getStatus() == OrderStatus.CANCELLED || 
            order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.REFUNDED) {
            throw new IllegalStateException("Cette commande ne peut pas être annulée");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        order.setLastModifiedAt(LocalDateTime.now());

        // Traitement du remboursement si demandé et possible
        if (processRefund && order.getStripePaymentIntentId() != null) {
            try {
                refundService.createRefund(orderId, order.getTotalAmount(), reason);
                order.setStatus(OrderStatus.REFUNDED);
                logger.info("Remboursement automatique traité pour commande {}", orderId);
            } catch (Exception e) {
                logger.error("Erreur lors du remboursement automatique: {}", e.getMessage());
                // Continue sans remboursement automatique
            }
        }

        order = orderRepository.save(order);

        // Historique et notification
        orderStatusHistoryService.recordStatusChange(order, oldStatus, order.getStatus(),
            "Annulation admin: " + reason);
        notificationService.sendOrderCancellationNotification(order, reason);

        OrderDto orderDto = convertToDto(order);
        publishOrderUpdated(order, orderDto, oldStatus.toString(), order.getStatus().toString());

        return orderDto;
    }

    /**
     * Met à jour les notes administratives
     */
    public OrderDto updateAdminNotes(java.util.UUID orderId, String adminNotes) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        order.setAdminNotes(adminNotes);
        order.setLastModifiedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        logger.info("Notes admin mises à jour pour commande {}", orderId);
        return convertToDto(order);
    }

    /**
     * Met à jour la priorité d'une commande
     */
    public OrderDto updateOrderPriority(java.util.UUID orderId, Integer priority) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        order.setPriority(priority);
        order.setLastModifiedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        logger.info("Priorité commande {} mise à jour: {}", orderId, priority);
        return convertToDto(order);
    }

    /**
     * Met à jour les tags d'une commande
     */
    public OrderDto updateOrderTags(java.util.UUID orderId, String tags) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        order.setTags(tags);
        order.setLastModifiedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        logger.info("Tags commande {} mis à jour: {}", orderId, tags);
        return convertToDto(order);
    }

    // ========== Synchronisation Stripe ==========

    /**
     * Synchronise une commande avec Stripe
     */
    public OrderDto syncWithStripe(java.util.UUID orderId) {
        logger.info("Synchronisation Stripe pour commande {}", orderId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        if (order.getStripePaymentIntentId() == null) {
            throw new IllegalStateException("Aucun PaymentIntent Stripe associé");
        }

        try {
            OrderStatus statusBeforeSync = order.getStatus();
            // Récupération des informations Stripe
            PaymentIntent paymentIntent = PaymentIntent.retrieve(order.getStripePaymentIntentId());
            
            // Mise à jour du statut de paiement
            String oldPaymentStatus = order.getPaymentStatus();
            order.setPaymentStatus(paymentIntent.getStatus());
            order.setLastModifiedAt(LocalDateTime.now());

            // Mise à jour du statut de commande si nécessaire
            if ("succeeded".equals(paymentIntent.getStatus()) && 
                order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPaidAt(LocalDateTime.now());
            }

            order = orderRepository.save(order);

            if (statusBeforeSync != order.getStatus()) {
                orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, statusBeforeSync,
                        order.getStatus());
            }
            
            logger.info("Synchronisation Stripe réussie: {} -> {}", 
                oldPaymentStatus, order.getPaymentStatus());

        } catch (StripeException e) {
            logger.error("Erreur synchronisation Stripe: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la synchronisation avec Stripe: " + e.getMessage());
        }

        return convertToDto(order);
    }

    // ========== Statistiques et Rapports ==========

    /**
     * Récupère le nombre total de commandes
     */
    @Transactional(readOnly = true)
    public Long getTotalOrdersCount() {
        return orderRepository.count();
    }

    /**
     * Récupère le chiffre d'affaires total
     */
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        try {
            List<Order> allOrders = orderRepository.findAll();
            return allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED ||
                               order.getStatus() == OrderStatus.CONFIRMED)
                .filter(order -> order.getTotalAmount() != null)
                .mapToDouble(order -> order.getTotalAmount().doubleValue())
                .sum();
        } catch (Exception e) {
            logger.warn("Erreur lors du calcul du chiffre d'affaires: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Récupère le nombre de commandes par statut
     */
    @Transactional(readOnly = true)
    public Long getOrdersCountByStatus(OrderStatus status) {
        try {
            List<Order> allOrders = orderRepository.findAll();
            return allOrders.stream()
                .filter(order -> order.getStatus() == status)
                .count();
        } catch (Exception e) {
            logger.warn("Erreur lors du comptage des commandes par statut: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Récupère le chiffre d'affaires de la période
     */
    @Transactional(readOnly = true)
    public Double getRevenueForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Order> allOrders = orderRepository.findAll();
            return allOrders.stream()
                .filter(order -> order.getCreatedAt() != null)
                .filter(order -> order.getCreatedAt().isAfter(startDate) &&
                               order.getCreatedAt().isBefore(endDate))
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED ||
                               order.getStatus() == OrderStatus.CONFIRMED)
                .filter(order -> order.getTotalAmount() != null)
                .mapToDouble(order -> order.getTotalAmount().doubleValue())
                .sum();
        } catch (Exception e) {
            logger.warn("Erreur lors du calcul du chiffre d'affaires de la période: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Génère un rapport de synthèse pour la période
     */
    @Transactional(readOnly = true)
    public OrderReportDto generateReport(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Génération rapport commandes: {} à {}", startDate, endDate);
        return reportsService.generateOrderReport(startDate, endDate);
    }

    /**
     * Exporte les commandes selon les critères
     */
    @Transactional(readOnly = true)
    public List<OrderDto> exportOrders(LocalDateTime startDate, LocalDateTime endDate, OrderStatus status) {
        List<Order> orders = orderRepository.findOrdersForExport(startDate, endDate, status);
        return orders.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    // ========== Actions en lot ==========

    /**
     * Traite plusieurs commandes en lot
     */
    public List<OrderDto> processBulkAction(List<java.util.UUID> orderIds, OrderActionDto action) {
        logger.info("Action en lot {} sur {} commandes", action.getActionType(), orderIds.size());

        return orderIds.stream()
            .map(orderId -> {
                try {
                    return processSingleAction(orderId, action);
                } catch (Exception e) {
                    logger.error("Erreur action en lot sur commande {}: {}", orderId, e.getMessage());
                    return null;
                }
            })
            .filter(result -> result != null)
            .collect(Collectors.toList());
    }

    // ========== Méthodes utilitaires privées ==========

    private OrderDto processSingleAction(java.util.UUID orderId, OrderActionDto action) {
        switch (action.getActionType()) {
            case "CHANGE_STATUS":
                OrderStatus newStatus = OrderStatus.valueOf(action.getNewStatus().toUpperCase());
                return changeOrderStatus(orderId, newStatus, action.getNote());
            case "CANCEL":
                return cancelOrder(orderId, action.getNote(), action.isProcessRefund());
            case "UPDATE_PRIORITY":
                return updateOrderPriority(orderId, action.getPriority());
            case "UPDATE_TAGS":
                return updateOrderTags(orderId, action.getTags());
            case "SYNC_STRIPE":
                return syncWithStripe(orderId);
            default:
                throw new IllegalArgumentException("Action non supportée: " + action.getActionType());
        }
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Logique de validation des transitions de statut
        if (from == OrderStatus.CANCELLED || from == OrderStatus.REFUNDED) {
            throw new IllegalStateException("Impossible de changer le statut d'une commande annulée ou remboursée");
        }

        if (to == OrderStatus.DELIVERED && from != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Une commande doit être expédiée avant d'être livrée");
        }

        // Autres validations selon la logique métier...
    }

    private void updateStatusSpecificFields(Order order, OrderStatus newStatus) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (newStatus) {
            case CONFIRMED:
                if (order.getPaidAt() == null) {
                    order.setPaidAt(now);
                }
                break;
            case PROCESSING:
                // Pas de champ spécifique
                break;
            case SHIPPED:
                order.setShippedAt(now);
                break;
            case DELIVERED:
                order.setDeliveredAt(now);
                break;
            case CANCELLED:
                order.setCancelledAt(now);
                break;
            case REFUNDED:
                // Géré par le RefundService
                break;
        }
    }

    private void publishOrderUpdated(Order order, OrderDto dto, String oldStatus, String newStatus) {
        Map<String, Object> pl = new HashMap<>();
        pl.put(BusinessEventPayloadKeys.ORDER_ID, order.getId().toString());
        if (order.getUser() != null) {
            pl.put(BusinessEventPayloadKeys.USER_ID, order.getUser().getId().toString());
        }
        pl.put(BusinessEventPayloadKeys.CUSTOMER_NAME, dto.getCustomerName());
        pl.put(BusinessEventPayloadKeys.SERVICE_NAME, dto.getServiceName());
        if (dto.getAmount() != null) {
            pl.put(BusinessEventPayloadKeys.AMOUNT, dto.getAmount().doubleValue());
        }
        pl.put(BusinessEventPayloadKeys.OLD_STATUS, oldStatus);
        pl.put(BusinessEventPayloadKeys.NEW_STATUS, newStatus);
        String line = String.format("Commande %s : %s → %s", order.getId(), oldStatus, newStatus);
        pl.put(BusinessEventPayloadKeys.MESSAGE, line);
        pl.put(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE, line);
        if (order.getUser() != null) {
            pl.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.TRUE);
            pl.put(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE, "STATUS_CHANGED");
        }
        eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.ORDER_UPDATED, "billing", order.getId(), pl));
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        
        // Copie des champs de base
        dto.setId(order.getId());
        dto.setServiceName(order.getServiceName());
        dto.setAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setPriority(order.getPriority());
        dto.setTags(order.getTags());
        dto.setAdminNotes(order.getAdminNotes());
        
        // Informations client
        if (order.getUser() != null) {
            dto.setCustomerEmail(order.getUser().getEmail());
            dto.setCustomerName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
        }
        
        // Informations Stripe
        dto.setStripeSessionId(order.getStripeSessionId());
        dto.setStripePaymentIntentId(order.getStripePaymentIntentId());
        
        // Dates importantes
        dto.setCreatedAt(order.getCreatedAt());
        dto.setPaidAt(order.getPaidAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setLastModifiedAt(order.getLastModifiedAt());
        
        // Métadonnées
        dto.setCancellationReason(order.getCancellationReason());
        dto.setProcessingNotes(order.getProcessingNotes());
        
        // Adresse de facturation
        dto.setBillingAddress(order.getBillingAddress());
        dto.setBillingCity(order.getBillingCity());
        dto.setBillingPostalCode(order.getBillingPostalCode());
        dto.setBillingCountry(order.getBillingCountry());
        
        // Calcul des métriques
        if (order.getCreatedAt() != null && order.getPaidAt() != null) {
            dto.setPaymentDelayHours(
                java.time.Duration.between(order.getCreatedAt(), order.getPaidAt()).toHours());
        }
        
        // Statut de remboursement
        dto.setHasRefunds(!order.getRefunds().isEmpty());
        
        return dto;
    }
}