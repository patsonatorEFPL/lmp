package com.lmp.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.lmp.billing.dto.admin.OrderDto;
import com.lmp.notification.service.InAppNotificationService;
import com.lmp.notification.domain.InAppNotification;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour les notifications WebSocket temps réel
 * Envoie des notifications aux administrateurs pour les changements de commandes.
 * Persiste aussi les notifications in-app pour les utilisateurs.
 */
@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final InAppNotificationService inAppNotificationService;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate,
                                         InAppNotificationService inAppNotificationService) {
        this.messagingTemplate = messagingTemplate;
        this.inAppNotificationService = inAppNotificationService;
    }

    /**
     * Notifie les administrateurs d'une nouvelle commande
     */
    public void notifyNewOrder(OrderDto order) {
        Map<String, Object> notification = Map.of(
            "type", "NEW_ORDER",
            "orderId", order.getId(),
            "customerName", order.getCustomerName(),
            "amount", order.getAmount(),
            "serviceName", order.getServiceName(),
            "timestamp", LocalDateTime.now(),
            "message", "Nouvelle commande reçue de " + order.getCustomerName()
        );
        
        messagingTemplate.convertAndSend("/topic/admin/orders", notification);
    }

    /**
     * Notifie les administrateurs d'un changement de statut de commande
     */
    public void notifyOrderStatusChanged(OrderDto order, String oldStatus, String newStatus) {
        Map<String, Object> notification = Map.of(
            "type", "STATUS_CHANGED",
            "orderId", order.getId(),
            "customerName", order.getCustomerName(),
            "oldStatus", oldStatus,
            "newStatus", newStatus,
            "timestamp", LocalDateTime.now(),
            "message", String.format("Commande #%d: %s → %s", order.getId(), oldStatus, newStatus)
        );
        
        messagingTemplate.convertAndSend("/topic/admin/orders", notification);
    }

    /**
     * Notifie les administrateurs d'un paiement réussi
     */
    public void notifyPaymentSuccess(OrderDto order) {
        Map<String, Object> notification = Map.of(
            "type", "PAYMENT_SUCCESS",
            "orderId", order.getId(),
            "customerName", order.getCustomerName(),
            "amount", order.getAmount(),
            "timestamp", LocalDateTime.now(),
            "message", String.format("Paiement de %.2f€ confirmé pour la commande #%d", order.getAmount(), order.getId())
        );
        
        messagingTemplate.convertAndSend("/topic/admin/orders", notification);
    }

    /**
     * Notifie les administrateurs d'un remboursement
     */
    public void notifyRefund(OrderDto order, Double refundAmount) {
        Map<String, Object> notification = Map.of(
            "type", "REFUND",
            "orderId", order.getId(),
            "customerName", order.getCustomerName(),
            "refundAmount", refundAmount,
            "timestamp", LocalDateTime.now(),
            "message", String.format("Remboursement de %.2f€ effectué pour la commande #%s", refundAmount, order.getId())
        );
        
        messagingTemplate.convertAndSend("/topic/admin/orders", notification);
    }

    /**
     * Notifie les administrateurs d'une erreur de paiement
     */
    public void notifyPaymentError(java.util.UUID orderId, String customerName, String error) {
        Map<String, Object> notification = Map.of(
            "type", "PAYMENT_ERROR",
            "orderId", orderId,
            "customerName", customerName,
            "error", error,
            "timestamp", LocalDateTime.now(),
            "message", String.format("Erreur de paiement pour la commande #%s: %s", orderId, error)
        );
        
        messagingTemplate.convertAndSend("/topic/admin/orders", notification);
    }

    /**
     * Notifie un utilisateur spécifique d'une nouvelle commande en attente de paiement.
     * Persiste la notification en base de données pour la retrouver après rechargement.
     */
    public void notifyUserNewPendingOrder(String userId, String orderId, String serviceName, Double amount) {
        String type = "NEW_PENDING_ORDER";
        String message = String.format("Nouvelle commande en attente : %s (%.2f€)", serviceName, amount);

        // 1. Persist notification in DB
        String persistedId = null;
        try {
            InAppNotification persisted = inAppNotificationService.createNotification(
                    userId, type, message, orderId, serviceName, amount);
            persistedId = persisted.getId().toString();
        } catch (Exception e) {
            logger.error("Failed to persist in-app notification for user {}: {}", userId, e.getMessage());
        }

        // 2. Send via WebSocket with the persisted ID (so frontend uses DB id)
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("orderId", orderId);
        notification.put("serviceName", serviceName);
        notification.put("amount", amount);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("message", message);
        if (persistedId != null) {
            notification.put("id", persistedId);
        }

        // Send to specific user queue
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
        // Also broadcast to topic for the user (fallback)
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
    }

    /**
     * Envoie les statistiques mises à jour en temps réel
     */
    public void notifyStatsUpdate(Map<String, Object> stats) {
        Map<String, Object> notification = Map.of(
            "type", "STATS_UPDATE",
            "stats", stats,
            "timestamp", LocalDateTime.now()
        );
        
        messagingTemplate.convertAndSend("/topic/admin/stats", notification);
    }

    /**
     * Notifie une mise à jour générale du dashboard
     */
    public void notifyDashboardUpdate() {
        Map<String, Object> notification = Map.of(
            "type", "DASHBOARD_UPDATE",
            "timestamp", LocalDateTime.now(),
            "message", "Dashboard mis à jour"
        );
        
        messagingTemplate.convertAndSend("/topic/admin/dashboard", notification);
    }
}