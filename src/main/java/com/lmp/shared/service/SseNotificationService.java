package com.lmp.shared.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lmp.integration.event.BusinessEventAdminMessageDefaults;
import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.integration.event.BusinessEventSseDispatchPolicy;
import com.lmp.integration.event.BusinessEventSseNames;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.notification.domain.InAppNotification;
import com.lmp.notification.service.InAppNotificationService;

/**
 * Notifications temps réel via SSE (+ persistance in-app pour les clients).
 * Les événements métier sont dispatchés après commit via {@link SseBusinessEventListener}.
 * <p>
 * Routage : enrichissement transport + diffusion admin ; le détail métier (messages, types in-app)
 * est porté par le payload ({@link BusinessEventPayloadKeys}) et les classes {@code BusinessEvent*} du module integration.
 */
@Service
public class SseNotificationService {

    private static final String ADMIN_UNIFIED_EVENT = "lmp-admin";

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationService.class);

    private final SseEmitterManager emitterManager;
    private final InAppNotificationService inAppNotificationService;

    public SseNotificationService(SseEmitterManager emitterManager,
                                   InAppNotificationService inAppNotificationService) {
        this.emitterManager = emitterManager;
        this.inAppNotificationService = inAppNotificationService;
    }

    /**
     * Point d'entrée unique depuis le bus métier (après commit).
     */
    public void dispatchFromBusinessEvent(LmpBusinessEvent e) {
        Map<String, Object> payload = e.payload() != null ? new HashMap<>(e.payload()) : new HashMap<>();
        payload.put("event", BusinessEventSseNames.canonicalSseEvent(e.type()));
        payload.put("businessType", e.type().name());
        payload.put("eventId", e.eventId().toString());
        payload.put("entityId", e.entityId() != null ? e.entityId().toString() : null);
        payload.put("timestamp", e.timestamp() != null ? e.timestamp().toString() : LocalDateTime.now().toString());

        BusinessEventAdminMessageDefaults.ensureAdminMessage(e.type(), payload);

        emitterManager.sendToAdmins(ADMIN_UNIFIED_EVENT, payload);

        if (BusinessEventSseDispatchPolicy.includeDashboardPing(e.type(), payload)) {
            notifyDashboardUpdate();
        }

        maybeNotifyUserFromPayload(payload);
    }

    private void maybeNotifyUserFromPayload(Map<String, Object> payload) {
        if (!Boolean.TRUE.equals(payload.get(BusinessEventPayloadKeys.NOTIFY_USER))) {
            return;
        }
        Object userId = payload.get(BusinessEventPayloadKeys.USER_ID);
        if (userId == null) {
            return;
        }
        String notifType = Objects.toString(payload.get(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE), "");
        if (notifType.isEmpty()) {
            logger.warn("notifyUser sans inAppNotificationType (payload eventId={})", payload.get("eventId"));
            return;
        }
        Object orderId = payload.get(BusinessEventPayloadKeys.ORDER_ID);
        String message = Objects.toString(payload.get(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE), null);
        if (message == null || message.isEmpty()) {
            message = Objects.toString(payload.get(BusinessEventPayloadKeys.MESSAGE), "Notification");
        }
        String serviceName = Objects.toString(payload.get(BusinessEventPayloadKeys.SERVICE_NAME), null);
        Double amount = payload.get(BusinessEventPayloadKeys.AMOUNT) instanceof Number n
                ? n.doubleValue() : null;
        persistAndSendUserNotification(userId.toString(), notifType, message,
                orderId != null ? orderId.toString() : null, serviceName, amount, payload);
    }

    private void persistAndSendUserNotification(String userId, String type, String message, String orderId,
                                                 String serviceName, Double amount, Map<String, Object> payload) {
        String persistedId = null;
        try {
            InAppNotification persisted = inAppNotificationService.createNotification(
                    userId, type, message, orderId, serviceName, amount);
            persistedId = persisted.getId().toString();
        } catch (Exception ex) {
            logger.error("Failed to persist in-app notification: {}", ex.getMessage());
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", java.time.Instant.now().toString());
        notification.put("event", payload.get("event"));
        notification.put("eventId", payload.get("eventId"));
        if (orderId != null) {
            notification.put("orderId", orderId);
        }
        if (serviceName != null) {
            notification.put("serviceName", serviceName);
        }
        if (amount != null) {
            notification.put("amount", amount);
        }
        if (persistedId != null) {
            notification.put("id", persistedId);
        }

        emitterManager.sendToUser(userId, "notification", notification);
    }

    public void notifyStatsUpdate(Map<String, Object> stats) {
        Map<String, Object> notification = Map.of(
                "type", "STATS_UPDATE",
                "stats", stats,
                "timestamp", LocalDateTime.now().toString()
        );
        emitterManager.sendToAdmins("admin-stats", notification);
    }

    public void notifyDashboardUpdate() {
        Map<String, Object> notification = Map.of(
                "type", "DASHBOARD_UPDATE",
                "timestamp", LocalDateTime.now().toString(),
                "message", "Dashboard mis à jour"
        );
        emitterManager.sendToAdmins("admin-dashboard", notification);
    }
}
