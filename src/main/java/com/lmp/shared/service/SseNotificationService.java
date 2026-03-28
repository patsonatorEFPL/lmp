package com.lmp.shared.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lmp.billing.dto.admin.OrderDto;
import com.lmp.notification.domain.InAppNotification;
import com.lmp.notification.service.InAppNotificationService;

/**
 * Drop-in replacement for WebSocketNotificationService.
 * Same public API — sends via SseEmitterManager instead of SimpMessagingTemplate.
 * Also persists notifications via InAppNotificationService where applicable.
 */
@Service
public class SseNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationService.class);

    private final SseEmitterManager emitterManager;
    private final InAppNotificationService inAppNotificationService;

    public SseNotificationService(SseEmitterManager emitterManager,
                                   InAppNotificationService inAppNotificationService) {
        this.emitterManager = emitterManager;
        this.inAppNotificationService = inAppNotificationService;
    }

    /**
     * Notifie les administrateurs d'une nouvelle commande.
     */
    public void notifyNewOrder(OrderDto order) {
        Map<String, Object> notification = Map.of(
                "type", "NEW_ORDER",
                "orderId", order.getId(),
                "customerName", order.getCustomerName(),
                "amount", order.getAmount(),
                "serviceName", order.getServiceName(),
                "timestamp", LocalDateTime.now().toString(),
                "message", "Nouvelle commande reçue de " + order.getCustomerName()
        );

        emitterManager.sendToAdmins("admin-order", notification);
    }

    /**
     * Notifie les administrateurs d'un changement de statut de commande.
     */
    public void notifyOrderStatusChanged(OrderDto order, String oldStatus, String newStatus) {
        Map<String, Object> notification = Map.of(
                "type", "STATUS_CHANGED",
                "orderId", order.getId(),
                "customerName", order.getCustomerName(),
                "oldStatus", oldStatus,
                "newStatus", newStatus,
                "timestamp", LocalDateTime.now().toString(),
                "message", String.format("Commande #%d: %s → %s", order.getId(), oldStatus, newStatus)
        );

        emitterManager.sendToAdmins("admin-order", notification);
    }

    /**
     * Notifie les administrateurs d'un paiement réussi.
     */
    public void notifyPaymentSuccess(OrderDto order) {
        Map<String, Object> notification = Map.of(
                "type", "PAYMENT_SUCCESS",
                "orderId", order.getId(),
                "customerName", order.getCustomerName(),
                "amount", order.getAmount(),
                "timestamp", LocalDateTime.now().toString(),
                "message", String.format("Paiement de %.2f€ confirmé pour la commande #%d", order.getAmount(), order.getId())
        );

        emitterManager.sendToAdmins("admin-order", notification);
    }

    /**
     * Notifie les administrateurs d'un remboursement.
     */
    public void notifyRefund(OrderDto order, Double refundAmount) {
        Map<String, Object> notification = Map.of(
                "type", "REFUND",
                "orderId", order.getId(),
                "customerName", order.getCustomerName(),
                "refundAmount", refundAmount,
                "timestamp", LocalDateTime.now().toString(),
                "message", String.format("Remboursement de %.2f€ effectué pour la commande #%s", refundAmount, order.getId())
        );

        emitterManager.sendToAdmins("admin-order", notification);
    }

    /**
     * Notifie les administrateurs d'une erreur de paiement.
     */
    public void notifyPaymentError(java.util.UUID orderId, String customerName, String error) {
        Map<String, Object> notification = Map.of(
                "type", "PAYMENT_ERROR",
                "orderId", orderId,
                "customerName", customerName,
                "error", error,
                "timestamp", LocalDateTime.now().toString(),
                "message", String.format("Erreur de paiement pour la commande #%s: %s", orderId, error)
        );

        emitterManager.sendToAdmins("admin-order", notification);
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

        // 2. Send via SSE with the persisted ID (so frontend uses DB id)
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("orderId", orderId);
        notification.put("serviceName", serviceName);
        notification.put("amount", amount);
        notification.put("timestamp", java.time.Instant.now().toString());
        notification.put("message", message);
        if (persistedId != null) {
            notification.put("id", persistedId);
        }

        emitterManager.sendToUser(userId, "notification", notification);
    }

    // ========== APPOINTMENT NOTIFICATIONS ==========

    /**
     * Notifie un utilisateur d'un nouveau rendez-vous créé.
     */
    public void notifyUserNewAppointment(String userId, UUID appointmentId, String subject, String dateTime) {
        String type = "APPOINTMENT_CREATED";
        String message = String.format("Votre rendez-vous « %s » du %s est en attente de confirmation", subject, dateTime);

        String persistedId = persistNotification(userId, type, message, null, subject, null);

        Map<String, Object> notification = buildNotification(type, message, persistedId);
        notification.put("appointmentId", appointmentId.toString());
        notification.put("subject", subject);

        emitterManager.sendToUser(userId, "notification", notification);
    }

    /**
     * Notifie un utilisateur d'un changement de statut de rendez-vous.
     */
    public void notifyUserAppointmentStatusChanged(String userId, UUID appointmentId, String subject,
                                                     String oldStatus, String newStatus, String dateTime) {
        String type = "APPOINTMENT_STATUS_CHANGED";
        String message = String.format("Votre rendez-vous « %s » du %s : %s → %s", subject, dateTime, oldStatus, newStatus);

        String persistedId = persistNotification(userId, type, message, null, subject, null);

        Map<String, Object> notification = buildNotification(type, message, persistedId);
        notification.put("appointmentId", appointmentId.toString());
        notification.put("oldStatus", oldStatus);
        notification.put("newStatus", newStatus);

        emitterManager.sendToUser(userId, "notification", notification);
    }

    /**
     * Notifie un utilisateur de l'annulation de son rendez-vous.
     */
    public void notifyUserAppointmentCancelled(String userId, UUID appointmentId, String subject, String dateTime, String reason) {
        String type = "APPOINTMENT_CANCELLED";
        String message = String.format("Votre rendez-vous « %s » du %s a été annulé" +
                (reason != null ? " — Raison : " + reason : ""), subject, dateTime);

        String persistedId = persistNotification(userId, type, message, null, subject, null);

        Map<String, Object> notification = buildNotification(type, message, persistedId);
        notification.put("appointmentId", appointmentId.toString());

        emitterManager.sendToUser(userId, "notification", notification);
    }

    /**
     * Notifie les admins d'un nouveau rendez-vous.
     */
    public void notifyAdminNewAppointment(UUID appointmentId, String clientName, String subject, String dateTime) {
        Map<String, Object> notification = Map.of(
                "type", "ADMIN_NEW_APPOINTMENT",
                "appointmentId", appointmentId.toString(),
                "clientName", clientName,
                "subject", subject,
                "timestamp", java.time.Instant.now().toString(),
                "message", String.format("Nouveau rendez-vous de %s : « %s » le %s", clientName, subject, dateTime)
        );

        emitterManager.sendToAdmins("admin-appointment", notification);
    }

    // ========== USER NOTIFICATIONS ==========

    /**
     * Notifie les admins d'un nouvel utilisateur inscrit.
     */
    public void notifyAdminNewUser(UUID userId, String email, String displayName) {
        Map<String, Object> notification = Map.of(
                "type", "ADMIN_NEW_USER",
                "userId", userId.toString(),
                "email", email,
                "displayName", displayName != null ? displayName : email,
                "timestamp", java.time.Instant.now().toString(),
                "message", String.format("Nouvel utilisateur inscrit : %s (%s)", displayName != null ? displayName : email, email)
        );

        emitterManager.sendToAdmins("admin-user", notification);
    }

    // ========== REVIEW NOTIFICATIONS ==========

    /**
     * Notifie les admins d'un nouvel avis.
     */
    public void notifyAdminNewReview(UUID reviewId, String clientName, int rating) {
        Map<String, Object> notification = Map.of(
                "type", "ADMIN_NEW_REVIEW",
                "reviewId", reviewId.toString(),
                "clientName", clientName,
                "rating", rating,
                "timestamp", java.time.Instant.now().toString(),
                "message", String.format("Nouvel avis de %s : %d/5 ⭐", clientName, rating)
        );

        emitterManager.sendToAdmins("admin-review", notification);
    }

    // ========== HELPERS ==========

    /**
     * Persists a notification and returns the persisted ID, or null on failure.
     */
    private String persistNotification(String userId, String type, String message,
                                        String orderId, String serviceName, Double amount) {
        try {
            InAppNotification persisted = inAppNotificationService.createNotification(
                    userId, type, message, orderId, serviceName, amount);
            return persisted.getId().toString();
        } catch (Exception e) {
            logger.error("Failed to persist in-app notification for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Builds a standard notification map with optional persisted ID.
     */
    private Map<String, Object> buildNotification(String type, String message, String persistedId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("timestamp", java.time.Instant.now().toString());
        notification.put("message", message);
        if (persistedId != null) {
            notification.put("id", persistedId);
        }
        return notification;
    }

    /**
     * Envoie les statistiques mises à jour en temps réel.
     */
    public void notifyStatsUpdate(Map<String, Object> stats) {
        Map<String, Object> notification = Map.of(
                "type", "STATS_UPDATE",
                "stats", stats,
                "timestamp", LocalDateTime.now().toString()
        );

        emitterManager.sendToAdmins("admin-stats", notification);
    }

    /**
     * Notifie une mise à jour générale du dashboard.
     */
    public void notifyDashboardUpdate() {
        Map<String, Object> notification = Map.of(
                "type", "DASHBOARD_UPDATE",
                "timestamp", LocalDateTime.now().toString(),
                "message", "Dashboard mis à jour"
        );

        emitterManager.sendToAdmins("admin-dashboard", notification);
    }
}
