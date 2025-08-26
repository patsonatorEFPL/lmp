# Spécifications Techniques - Vue Administration des Commandes

## 📋 DTOs (Data Transfer Objects)

### 1. OrderAdminDto
```java
package com.lmp.web.dto.admin;

import com.lmp.domain.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour l'affichage et la gestion des commandes en interface admin
 */
public class OrderAdminDto {
    
    private Long id;
    
    @NotBlank(message = "Le nom du client est requis")
    private String customerName;
    
    @Email(message = "Format d'email invalide")
    private String customerEmail;
    
    private Long customerId;
    
    @NotNull(message = "Le statut est requis")
    private OrderStatus status;
    
    @NotNull(message = "Le montant total est requis")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private BigDecimal totalAmount;
    
    private String currency = "CAD";
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String lastUpdatedBy;
    
    private String stripeSessionId;
    
    private String notes;
    
    // Relations
    private List<OrderItemAdminDto> items;
    private List<PaymentTransactionDto> transactions;
    private List<OrderStatusHistoryDto> statusHistory;
    
    // Flags métier
    private boolean canBeModified;
    private boolean canBeCancelled;
    private boolean canBeRefunded;
    private boolean hasUnresolvedIssues;
    
    // Métriques calculées
    private BigDecimal totalPaid;
    private BigDecimal totalRefunded;
    private BigDecimal outstandingAmount;
    private int daysSinceCreation;
    
    // Constructeurs
    public OrderAdminDto() {}
    
    public OrderAdminDto(Long id, String customerName, String customerEmail, 
                        OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        
        // Calcul des flags métier
        this.canBeModified = calculateCanBeModified();
        this.canBeCancelled = calculateCanBeCancelled();
        this.canBeRefunded = calculateCanBeRefunded();
    }
    
    // Méthodes métier
    public boolean calculateCanBeModified() {
        return status != OrderStatus.COMPLETED && 
               status != OrderStatus.CANCELLED;
    }
    
    public boolean calculateCanBeCancelled() {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.IN_PROGRESS ||
               status == OrderStatus.UNDER_REVIEW;
    }
    
    public boolean calculateCanBeRefunded() {
        return (status == OrderStatus.COMPLETED || 
                status == OrderStatus.IN_PROGRESS) &&
               totalPaid != null && 
               totalPaid.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public String getStatusDisplayName() {
        return switch(status) {
            case PAYMENT_PENDING -> "En attente de paiement";
            case PENDING -> "En attente";
            case IN_PROGRESS -> "En cours";
            case COMPLETED -> "Terminée";
            case UNDER_REVIEW -> "En révision";
            case CANCELLED -> "Annulée";
        };
    }
    
    public String getStatusBadgeClass() {
        return switch(status) {
            case PAYMENT_PENDING -> "badge-warning";
            case PENDING -> "badge-info";
            case IN_PROGRESS -> "badge-primary";
            case COMPLETED -> "badge-success";
            case UNDER_REVIEW -> "badge-secondary";
            case CANCELLED -> "badge-danger";
        };
    }
    
    // Getters et Setters complets...
    // [Implémentation complète omise pour la brièveté]
}
```

### 2. OrderFilterDto
```java
package com.lmp.web.dto.admin;

import com.lmp.domain.enums.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour les filtres de recherche avancée des commandes
 */
public class OrderFilterDto {
    
    // Filtres par statut
    private List<OrderStatus> statuses;
    private boolean includePaymentPending = true;
    
    // Filtres temporels
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;
    
    private String datePreset; // "today", "week", "month", "quarter", "year"
    
    // Filtres utilisateur
    private String userSearch; // Nom, email ou ID
    private Long specificUserId;
    
    // Filtres montant
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String amountRange; // "0-100", "100-500", "500-1000", "1000+"
    
    // Filtres paiement
    private String paymentProvider; // "stripe", "paypal", etc.
    private String paymentStatus;
    
    // Filtres métier
    private Boolean hasRefunds;
    private Boolean hasDisputes;
    private Boolean hasNotes;
    private Integer minDaysSinceCreation;
    private Integer maxDaysSinceCreation;
    
    // Tri et pagination
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
    private int page = 0;
    private int size = 20;
    
    // Filtres sauvegardés
    private String savedFilterName;
    private boolean saveCurrentFilter;
    
    // Constructeurs
    public OrderFilterDto() {
        // Valeurs par défaut
        this.startDate = LocalDateTime.now().minusDays(30);
        this.endDate = LocalDateTime.now();
    }
    
    // Méthodes utilitaires
    public boolean hasDateFilter() {
        return startDate != null || endDate != null;
    }
    
    public boolean hasAmountFilter() {
        return minAmount != null || maxAmount != null || amountRange != null;
    }
    
    public boolean hasUserFilter() {
        return userSearch != null || specificUserId != null;
    }
    
    public boolean isEmpty() {
        return (statuses == null || statuses.isEmpty()) &&
               !hasDateFilter() &&
               !hasAmountFilter() &&
               !hasUserFilter() &&
               paymentProvider == null &&
               paymentStatus == null;
    }
    
    public void applyDatePreset() {
        LocalDateTime now = LocalDateTime.now();
        switch(datePreset != null ? datePreset : "month") {
            case "today" -> {
                startDate = now.toLocalDate().atStartOfDay();
                endDate = now;
            }
            case "week" -> {
                startDate = now.minusDays(7);
                endDate = now;
            }
            case "month" -> {
                startDate = now.minusDays(30);
                endDate = now;
            }
            case "quarter" -> {
                startDate = now.minusDays(90);
                endDate = now;
            }
            case "year" -> {
                startDate = now.minusDays(365);
                endDate = now;
            }
        }
    }
    
    public void applyAmountRange() {
        if (amountRange != null) {
            switch(amountRange) {
                case "0-100" -> {
                    minAmount = BigDecimal.ZERO;
                    maxAmount = new BigDecimal("100");
                }
                case "100-500" -> {
                    minAmount = new BigDecimal("100");
                    maxAmount = new BigDecimal("500");
                }
                case "500-1000" -> {
                    minAmount = new BigDecimal("500");
                    maxAmount = new BigDecimal("1000");
                }
                case "1000+" -> {
                    minAmount = new BigDecimal("1000");
                    maxAmount = null;
                }
            }
        }
    }
    
    // Getters et Setters...
}
```

### 3. OrderActionDto
```java
package com.lmp.web.dto.admin;

import com.lmp.domain.enums.OrderStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO pour les actions administrateur sur les commandes
 */
public class OrderActionDto {
    
    @NotNull(message = "L'ID de la commande est requis")
    private Long orderId;
    
    @NotBlank(message = "Le type d'action est requis")
    private String actionType; // "UPDATE_STATUS", "CANCEL", "REFUND", "ADD_NOTE"
    
    private OrderStatus newStatus;
    
    @Size(min = 10, max = 1000, message = "La raison doit contenir entre 10 et 1000 caractères")
    private String reason;
    
    private String notes;
    
    // Pour les remboursements
    private BigDecimal refundAmount;
    private String refundReason;
    private boolean notifyCustomer = true;
    
    // Pour les modifications
    private BigDecimal newTotalAmount;
    private String adminOverrideReason;
    
    // Métadonnées
    private boolean requiresApproval;
    private String approverEmail;
    private String ipAddress;
    private String userAgent;
    
    // Constructeurs
    public OrderActionDto() {}
    
    public OrderActionDto(Long orderId, String actionType, String reason) {
        this.orderId = orderId;
        this.actionType = actionType;
        this.reason = reason;
    }
    
    // Méthodes de validation
    public boolean isValidForStatus() {
        return "UPDATE_STATUS".equals(actionType) && 
               newStatus != null && 
               reason != null && !reason.trim().isEmpty();
    }
    
    public boolean isValidForRefund() {
        return "REFUND".equals(actionType) && 
               refundAmount != null && 
               refundAmount.compareTo(BigDecimal.ZERO) > 0 &&
               refundReason != null && !refundReason.trim().isEmpty();
    }
    
    public boolean isValidForCancellation() {
        return "CANCEL".equals(actionType) && 
               reason != null && !reason.trim().isEmpty();
    }
    
    // Getters et Setters...
}
```

---

## 🔧 Services Détaillés

### 1. OrderAdminService
```java
package com.lmp.service.admin;

import com.lmp.domain.entity.*;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.repository.*;
import com.lmp.web.dto.admin.*;
import com.lmp.service.payment.PaymentService;
import com.lmp.service.notification.OrderNotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service principal pour l'administration des commandes
 */
@Service
@Transactional
public class OrderAdminService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderStatusHistoryRepository statusHistoryRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private OrderNotificationService notificationService;
    
    @Autowired
    private OrderAuditService auditService;
    
    /**
     * Recherche paginée avec filtres avancés
     */
    @Transactional(readOnly = true)
    public Page<OrderAdminDto> findOrdersWithFilters(OrderFilterDto filters, Pageable pageable) {
        
        // Application des presets si nécessaire
        if (filters.getDatePreset() != null) {
            filters.applyDatePreset();
        }
        
        if (filters.getAmountRange() != null) {
            filters.applyAmountRange();
        }
        
        // Requête avec filtres
        Page<Order> orders = orderRepository.findWithAdvancedFilters(
            filters.getStatuses(),
            filters.getStartDate(),
            filters.getEndDate(),
            filters.getUserSearch(),
            filters.getMinAmount(),
            filters.getMaxAmount(),
            pageable
        );
        
        // Conversion en DTOs avec données enrichies
        return orders.map(this::convertToAdminDto);
    }
    
    /**
     * Récupération d'une commande avec tous les détails
     */
    @Transactional(readOnly = true)
    public Optional<OrderAdminDto> getOrderDetails(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDetailedAdminDto);
    }
    
    /**
     * Mise à jour du statut d'une commande par un administrateur
     */
    public OrderAdminDto updateOrderStatus(OrderActionDto actionDto, String adminUserEmail) 
            throws OrderAdminException {
        
        // Validation
        if (!actionDto.isValidForStatus()) {
            throw new OrderAdminException("Données invalides pour la mise à jour du statut");
        }
        
        Order order = orderRepository.findById(actionDto.getOrderId())
                .orElseThrow(() -> new OrderAdminException("Commande non trouvée"));
        
        User adminUser = userRepository.findByEmail(adminUserEmail)
                .orElseThrow(() -> new OrderAdminException("Utilisateur administrateur non trouvé"));
        
        // Validation des transitions de statut
        validateStatusTransition(order.getStatus(), actionDto.getNewStatus());
        
        // Sauvegarde de l'ancien statut pour l'historique
        OrderStatus oldStatus = order.getStatus();
        
        // Mise à jour de la commande
        order.setStatus(actionDto.getNewStatus());
        order.setUpdatedAt(LocalDateTime.now());
        if (actionDto.getNotes() != null) {
            order.setNotes(actionDto.getNotes());
        }
        
        Order savedOrder = orderRepository.save(order);
        
        // Création de l'entrée d'historique
        createStatusHistoryEntry(savedOrder, oldStatus, actionDto.getNewStatus(), 
                                actionDto.getReason(), adminUser);
        
        // Audit de l'action
        auditService.logAdminAction(order.getId(), "UPDATE_STATUS", 
                                   adminUserEmail, actionDto.getReason(), 
                                   actionDto.getIpAddress());
        
        // Notification client si nécessaire
        if (shouldNotifyCustomerForStatus(actionDto.getNewStatus())) {
            notificationService.sendOrderStatusNotification(savedOrder, oldStatus);
        }
        
        return convertToAdminDto(savedOrder);
    }
    
    /**
     * Annulation d'une commande
     */
    public OrderAdminDto cancelOrder(OrderActionDto actionDto, String adminUserEmail) 
            throws OrderAdminException {
        
        if (!actionDto.isValidForCancellation()) {
            throw new OrderAdminException("Données invalides pour l'annulation");
        }
        
        Order order = orderRepository.findById(actionDto.getOrderId())
                .orElseThrow(() -> new OrderAdminException("Commande non trouvée"));
        
        // Validation que la commande peut être annulée
        if (!canOrderBeCancelled(order)) {
            throw new OrderAdminException("Cette commande ne peut pas être annulée");
        }
        
        User adminUser = userRepository.findByEmail(adminUserEmail)
                .orElseThrow(() -> new OrderAdminException("Utilisateur administrateur non trouvé"));
        
        OrderStatus oldStatus = order.getStatus();
        
        // Annulation
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setNotes(order.getNotes() + "\n[ANNULATION] " + actionDto.getReason());
        
        Order savedOrder = orderRepository.save(order);
        
        // Historique
        createStatusHistoryEntry(savedOrder, oldStatus, OrderStatus.CANCELLED, 
                                actionDto.getReason(), adminUser);
        
        // Audit
        auditService.logAdminAction(order.getId(), "CANCEL", 
                                   adminUserEmail, actionDto.getReason(), 
                                   actionDto.getIpAddress());
        
        // Notification
        notificationService.sendOrderCancellationNotification(savedOrder, actionDto.getReason());
        
        return convertToAdminDto(savedOrder);
    }
    
    /**
     * Processus de remboursement
     */
    public RefundResponseDto processRefund(OrderActionDto actionDto, String adminUserEmail) 
            throws OrderAdminException {
        
        if (!actionDto.isValidForRefund()) {
            throw new OrderAdminException("Données invalides pour le remboursement");
        }
        
        Order order = orderRepository.findById(actionDto.getOrderId())
                .orElseThrow(() -> new OrderAdminException("Commande non trouvée"));
        
        // Validation des conditions de remboursement
        validateRefundConditions(order, actionDto.getRefundAmount());
        
        try {
            // Traitement du remboursement via le service de paiement
            RefundRequestDto refundRequest = new RefundRequestDto();
            refundRequest.setAmount(actionDto.getRefundAmount());
            refundRequest.setReason(actionDto.getRefundReason());
            refundRequest.setNotifyCustomer(actionDto.isNotifyCustomer());
            
            // Récupération de la transaction principale
            PaymentTransaction originalTransaction = getOriginalPaymentTransaction(order);
            
            RefundResponseDto refundResponse = paymentService.refundPayment(
                originalTransaction.getId(), refundRequest);
            
            if (refundResponse.isSuccessful()) {
                // Audit du remboursement
                auditService.logAdminAction(order.getId(), "REFUND", 
                                           adminUserEmail, 
                                           String.format("Remboursement de %s CAD: %s", 
                                                        actionDto.getRefundAmount(), 
                                                        actionDto.getRefundReason()),
                                           actionDto.getIpAddress());
                
                // Notification client
                if (actionDto.isNotifyCustomer()) {
                    notificationService.sendRefundNotification(order, refundResponse);
                }
            }
            
            return refundResponse;
            
        } catch (Exception e) {
            auditService.logAdminAction(order.getId(), "REFUND_FAILED", 
                                       adminUserEmail, 
                                       "Échec du remboursement: " + e.getMessage(),
                                       actionDto.getIpAddress());
            throw new OrderAdminException("Échec du traitement du remboursement: " + e.getMessage());
        }
    }
    
    /**
     * Ajout de notes à une commande
     */
    public OrderAdminDto addOrderNote(Long orderId, String note, String adminUserEmail) 
            throws OrderAdminException {
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderAdminException("Commande non trouvée"));
        
        String timestamp = LocalDateTime.now().toString();
        String formattedNote = String.format("[%s - %s] %s", timestamp, adminUserEmail, note);
        
        String currentNotes = order.getNotes() != null ? order.getNotes() : "";
        order.setNotes(currentNotes + "\n" + formattedNote);
        order.setUpdatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        
        // Audit
        auditService.logAdminAction(orderId, "ADD_NOTE", adminUserEmail, note, null);
        
        return convertToAdminDto(savedOrder);
    }
    
    /**
     * Récupération des statistiques pour le dashboard
     */
    @Transactional(readOnly = true)
    public OrderDashboardStatsDto getDashboardStats() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        
        return OrderDashboardStatsDto.builder()
            .ordersToday(orderRepository.countByCreatedAtAfter(today))
            .ordersThisMonth(orderRepository.countByCreatedAtAfter(thisMonth))
            .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
            .inProgressOrders(orderRepository.countByStatus(OrderStatus.IN_PROGRESS))
            .underReviewOrders(orderRepository.countByStatus(OrderStatus.UNDER_REVIEW))
            .revenueToday(orderRepository.sumTotalAmountByCreatedAtAfter(today))
            .revenueThisMonth(orderRepository.sumTotalAmountByCreatedAtAfter(thisMonth))
            .averageOrderValue(orderRepository.calculateAverageOrderValue())
            .build();
    }
    
    // Méthodes utilitaires privées
    
    private OrderAdminDto convertToAdminDto(Order order) {
        OrderAdminDto dto = new OrderAdminDto();
        
        // Mapping de base
        dto.setId(order.getId());
        dto.setCustomerName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
        dto.setCustomerEmail(order.getUser().getEmail());
        dto.setCustomerId(order.getUser().getId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setStripeSessionId(order.getStripeSessionId());
        dto.setNotes(order.getNotes());
        
        // Calcul des métriques
        dto.setTotalPaid(calculateTotalPaid(order));
        dto.setTotalRefunded(calculateTotalRefunded(order));
        dto.setOutstandingAmount(dto.getTotalAmount().subtract(dto.getTotalPaid()));
        dto.setDaysSinceCreation(calculateDaysSinceCreation(order.getCreatedAt()));
        
        // Flags métier
        dto.setCanBeModified(canOrderBeModified(order));
        dto.setCanBeCancelled(canOrderBeCancelled(order));
        dto.setCanBeRefunded(canOrderBeRefunded(order));
        dto.setHasUnresolvedIssues(hasUnresolvedIssues(order));
        
        return dto;
    }
    
    private OrderAdminDto convertToDetailedAdminDto(Order order) {
        OrderAdminDto dto = convertToAdminDto(order);
        
        // Ajout des relations détaillées
        dto.setItems(convertOrderItems(order.getItems()));
        dto.setTransactions(convertPaymentTransactions(order.getPaymentTransactions()));
        dto.setStatusHistory(convertStatusHistory(order.getStatusHistories()));
        
        return dto;
    }
    
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) 
            throws OrderAdminException {
        
        // Définition des transitions autorisées
        boolean isValidTransition = switch(currentStatus) {
            case PAYMENT_PENDING -> List.of(OrderStatus.PENDING, OrderStatus.CANCELLED).contains(newStatus);
            case PENDING -> List.of(OrderStatus.IN_PROGRESS, OrderStatus.UNDER_REVIEW, OrderStatus.CANCELLED).contains(newStatus);
            case IN_PROGRESS -> List.of(OrderStatus.COMPLETED, OrderStatus.UNDER_REVIEW, OrderStatus.CANCELLED).contains(newStatus);
            case UNDER_REVIEW -> List.of(OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(newStatus);
            case COMPLETED -> false; // Pas de transition depuis COMPLETED
            case CANCELLED -> false; // Pas de transition depuis CANCELLED
        };
        
        if (!isValidTransition) {
            throw new OrderAdminException(
                String.format("Transition de statut non autorisée: %s -> %s", 
                             currentStatus, newStatus));
        }
    }
    
    private void createStatusHistoryEntry(Order order, OrderStatus oldStatus, 
                                        OrderStatus newStatus, String reason, User adminUser) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(newStatus);
        history.setNotes(String.format("Changement de statut: %s -> %s. Raison: %s", 
                                      oldStatus, newStatus, reason));
        history.setCreatedBy(adminUser);
        history.setCreatedAt(LocalDateTime.now());
        
        statusHistoryRepository.save(history);
    }
    
    private boolean canOrderBeModified(Order order) {
        return order.getStatus() != OrderStatus.COMPLETED && 
               order.getStatus() != OrderStatus.CANCELLED;
    }
    
    private boolean canOrderBeCancelled(Order order) {
        return order.getStatus() == OrderStatus.PENDING || 
               order.getStatus() == OrderStatus.IN_PROGRESS ||
               order.getStatus() == OrderStatus.UNDER_REVIEW;
    }
    
    private boolean canOrderBeRefunded(Order order) {
        if (order.getStatus() != OrderStatus.COMPLETED && 
            order.getStatus() != OrderStatus.IN_PROGRESS) {
            return false;
        }
        
        BigDecimal totalPaid = calculateTotalPaid(order);
        BigDecimal totalRefunded = calculateTotalRefunded(order);
        
        return totalPaid.subtract(totalRefunded).compareTo(BigDecimal.ZERO) > 0;
    }
    
    private boolean hasUnresolvedIssues(Order order) {
        // Logic pour détecter les problèmes non résolus
        return order.getStatus() == OrderStatus.UNDER_REVIEW ||
               (order.getStatus() == OrderStatus.PENDING && 
                calculateDaysSinceCreation(order.getCreatedAt()) > 7);
    }
    
    // Autres méthodes utilitaires...
}
```

### 2. OrderNotificationService
```java
package com.lmp.service.notification;

import com.lmp.domain.entity.Order;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.service.email.EmailService;
import com.lmp.service.email.dto.EmailTemplateDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service pour les notifications liées aux commandes
 */
@Service
public class OrderNotificationService {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationHistoryService historyService;
    
    @Autowired
    private WebSocketNotificationService webSocketService;
    
    /**
     * Notification de changement de statut
     */
    @Async
    public CompletableFuture<Void> sendOrderStatusNotification(Order order, OrderStatus oldStatus) {
        
        try {
            // Préparation des variables pour le template
            Map<String, Object> variables = prepareEmailVariables(order);
            variables.put("oldStatus", getStatusDisplayName(oldStatus));
            variables.put("newStatus", getStatusDisplayName(order.getStatus()));
            
            // Sélection du template approprié
            String templateName = getTemplateForStatus(order.getStatus());
            
            // Création du DTO email
            EmailTemplateDto emailDto = EmailTemplateDto.builder()
                .to(order.getUser().getEmail())
                .subject(getSubjectForStatus(order.getStatus(), order.getId()))
                .templateName(templateName)
                .variables(variables)
                .locale(order.getUser().getPreferredLocale())
                .build();
            
            // Envoi de l'email
            boolean sent = emailService.sendTemplatedEmail(emailDto);
            
            // Enregistrement dans l'historique
            historyService.recordNotification(order.getId(), "STATUS_CHANGE", 
                                            order.getUser().getEmail(), sent);
            
            // Notification temps réel pour l'admin
            webSocketService.notifyAdminOrderUpdate(order);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            // Log de l'erreur et enregistrement de l'échec
            historyService.recordNotificationFailure(order.getId(), "STATUS_CHANGE", 
                                                    e.getMessage());
            throw new NotificationException("Échec de l'envoi de notification", e);
        }
    }
    
    /**
     * Notification d'annulation
     */
    @Async
    public CompletableFuture<Void> sendOrderCancellationNotification(Order order, String reason) {
        
        Map<String, Object> variables = prepareEmailVariables(order);
        variables.put("cancellationReason", reason);
        variables.put("supportEmail", "support@lmp-digital.ca");
        
        EmailTemplateDto emailDto = EmailTemplateDto.builder()
            .to(order.getUser().getEmail())
            .subject("Annulation de votre commande #" + order.getId())
            .templateName("order-cancellation")
            .variables(variables)
            .locale(order.getUser().getPreferredLocale())
            .build();
        
        boolean sent = emailService.sendTemplatedEmail(emailDto);
        historyService.recordNotification(order.getId(), "CANCELLATION", 
                                        order.getUser().getEmail(), sent);
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Notification de remboursement
     */
    @Async
    public CompletableFuture<Void> sendRefundNotification(Order order, RefundResponseDto refundResponse) {
        
        Map<String, Object> variables = prepareEmailVariables(order);
        variables.put("refundAmount", refundResponse.getAmount());
        variables.put("refundId", refundResponse.getRefundId());
        variables.put("processingDays", "3-5");
        
        EmailTemplateDto emailDto = EmailTemplateDto.builder()
            .to(order.getUser().getEmail())
            .subject("Remboursement traité pour votre commande #" + order.getId())
            .templateName("order-refund")
            .variables(variables)
            .locale(order.getUser().getPreferredLocale())
            .build();
        
        boolean sent = emailService.sendTemplatedEmail(emailDto);
        historyService.recordNotification(order.getId(), "REFUND", 
                                        order.getUser().getEmail(), sent);
        
        return CompletableFuture.completedFuture(null);
    }
    
    // Méthodes utilitaires privées
    
    private Map<String, Object> prepareEmailVariables(Order order) {
        Map<String, Object> variables = new HashMap<>();
        
        variables.put("customerName", order.getUser().getFirstName());
        variables.put("orderId", order.getId());
        variables.put("orderTotal", order.getTotalAmount());
        variables.put("orderCurrency", "CAD");
        variables.put("orderDate", order.getCreatedAt());
        variables.put("orderStatus", getStatusDisplayName(order.getStatus()));
        variables.put("customerEmail", order.getUser().getEmail());
        variables.put("supportUrl", "https://lmp-digital.ca/support");
        variables.put("dashboardUrl", "https://lmp-digital.ca/dashboard");
        
        return variables;
    }
    
    private String getTemplateForStatus(OrderStatus status) {
        return switch(status) {
            case PENDING -> "order-confirmed";
            case IN_PROGRESS -> "order-in-progress";
            case COMPLETED -> "order-completed";
            case UNDER_REVIEW -> "order-under-review";
            case CANCELLED -> "order-cancelled";
            default -> "order-status-update";
        };
    }
    
    private String getSubjectForStatus(OrderStatus status, Long orderId) {
        return switch(status) {
            case PENDING -> "Confirmation de votre commande #" + orderId;
            case IN_PROGRESS -> "Votre commande #" + orderId + " est en cours de traitement";
            case COMPLETED -> "Votre commande #" + orderId + " est terminée";
            case UNDER_REVIEW -> "Votre commande #" + orderId + " est en révision";
            case CANCELLED -> "Votre commande #" + orderId + " a été annulée";
            default -> "Mise à jour de votre commande #" + orderId;
        };
    }
    
    private String getStatusDisplayName(OrderStatus status) {
        return switch(status) {
            case PAYMENT_PENDING -> "En attente de paiement";
            case PENDING -> "En attente";
            case IN_PROGRESS -> "En cours";
            case COMPLETED -> "Terminée";
            case UNDER_REVIEW -> "En révision";
            case CANCELLED -> "Annulée";
        };
    }
}
```

---

## 🗃️ Repositories Étendus

### 1. Extension OrderRepository
```java
package com.lmp.repository;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Méthodes existantes conservées
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserAndStatus(User user, OrderStatus status);
    Optional<Order> findByStripeSessionId(String stripeSessionId);
    List<Order> findByUserAndStatusNotOrderByCreatedAtDesc(User user, OrderStatus status);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);
    
    // Nouvelles méthodes pour l'administration
    
    /**
     * Recherche avancée avec filtres multiples
     */
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.user u
        LEFT JOIN FETCH o.paymentTransactions pt
        WHERE (:statuses IS NULL OR o.status IN :statuses)
        AND (:startDate IS NULL OR o.createdAt >= :startDate)
        AND (:endDate IS NULL OR o.createdAt <= :endDate)
        AND (:userSearch IS NULL OR 
             LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :userSearch, '%')) OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :userSearch, '%')))
        AND (:minAmount IS NULL OR o.totalAmount >= :minAmount)
        AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount)
        ORDER BY o.createdAt DESC
    """)
    Page<Order> findWithAdvancedFilters(
        @Param("statuses") List<OrderStatus> statuses,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("userSearch") String userSearch,
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount,
        Pageable pageable
    );
    
    /**
     * Statistiques pour le dashboard
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :date")
    BigDecimal sumTotalAmountByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT AVG(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal calculateAverageOrderValue();
    
    /**
     * Analytics et rapports
     */
    @Query("""
        SELECT NEW com.lmp.dto.admin.OrderAnalyticsDto(
            DATE(o.createdAt),
            COUNT(o),
            SUM(o.totalAmount),
            AVG(o.totalAmount)
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<OrderAnalyticsDto> getOrderAnalyticsByDate(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("""
        SELECT NEW com.lmp.dto.admin.OrderStatusAnalyticsDto(
            o.status,
            COUNT(o),
            SUM(o.totalAmount)
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        GROUP BY o.status
    """)
    List<OrderStatusAnalyticsDto> getOrderAnalyticsByStatus(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Recherche pour l'autocomplétion
     */
    @Query("""
        SELECT DISTINCT CONCAT(u.firstName, ' ', u.lastName, ' (', u.email, ')')
        FROM Order o JOIN o.user u
        WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName, ' ', u.email)) 
              LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY u.firstName, u.lastName
    """)
    List<String> findUserSuggestionsForSearch(@Param("search") String search);
    
    /**
     * Commandes nécessitant une attention
     */
    @Query("""
        SELECT o FROM Order o
        WHERE (o.status = 'UNDER_REVIEW') OR
              (o.status = 'PENDING' AND o.createdAt < :cutoffDate) OR
              (o.status = 'IN_PROGRESS' AND o.createdAt < :staleDate)
        ORDER BY o.createdAt ASC
    """)
    List<Order> findOrdersRequiringAttention(
        @Param("cutoffDate") LocalDateTime cutoffDate,
        @Param("staleDate") LocalDateTime staleDate
    );
    
    /**
     * Top clients par volume de commandes
     */
    @Query("""
        SELECT NEW com.lmp.dto.admin.TopCustomerDto(
            u.id,
            CONCAT(u.firstName, ' ', u.lastName),
            u.email,
            COUNT(o),
            SUM(o.totalAmount)
        )
        FROM Order o JOIN o.user u
        WHERE o.status = 'COMPLETED'
        AND o.createdAt >= :startDate
        GROUP BY u.id, u.firstName, u.lastName, u.email
        ORDER BY SUM(o.totalAmount) DESC
    """)
    List<TopCustomerDto> findTopCustomersByRevenue(
        @Param("startDate") LocalDateTime startDate,
        Pageable pageable
    );
}
```

---

Cette spécification technique détaille l'implémentation complète des DTOs et services pour la vue d'administration des commandes, en respectant l'architecture existante et les bonnes pratiques Spring Boot.