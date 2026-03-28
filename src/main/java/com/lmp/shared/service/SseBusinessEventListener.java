package com.lmp.shared.service;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.lmp.integration.event.LmpBusinessEvent;

/**
 * Central listener that converts business events into SSE notifications.
 *
 * Uses @TransactionalEventListener(AFTER_COMMIT) to guarantee that:
 * 1. The entity has been committed to the database
 * 2. The SSE notification won't refer to non-existent data
 * 3. No race conditions between frontend fetch and backend commit
 *
 * Architecture:
 *   Service → ApplicationEventPublisher → LmpBusinessEvent
 *       → SseBusinessEventListener (AFTER_COMMIT) → SseNotificationService → SSE
 */
@Component
public class SseBusinessEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SseBusinessEventListener.class);
    private static final DateTimeFormatter FR_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final SseNotificationService sseNotificationService;

    public SseBusinessEventListener(SseNotificationService sseNotificationService) {
        this.sseNotificationService = sseNotificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBusinessEvent(LmpBusinessEvent event) {
        logger.debug("SSE dispatch for event: {} (entity={})", event.type(), event.entityId());

        try {
            switch (event.type()) {
                // ========== CRM — Appointments ==========
                case APPOINTMENT_CREATED -> handleAppointmentCreated(event);
                case APPOINTMENT_CONFIRMED -> handleAppointmentConfirmed(event);
                case APPOINTMENT_CANCELLED -> handleAppointmentCancelled(event);

                // ========== Auth — Users ==========
                case USER_REGISTERED -> handleUserRegistered(event);

                // ========== Reviews ==========
                case REVIEW_CREATED -> handleReviewCreated(event);

                default -> logger.debug("No SSE handler for event type: {}", event.type());
            }
        } catch (Exception e) {
            logger.error("Error dispatching SSE for event {} (entity={}): {}",
                    event.type(), event.entityId(), e.getMessage(), e);
        }
    }

    // ========== Appointment Handlers ==========

    private void handleAppointmentCreated(LmpBusinessEvent event) {
        Map<String, Object> payload = event.payload();
        String userId = getStringPayload(payload, "userId");
        String subject = getStringPayload(payload, "subject");
        String dateTime = getStringPayload(payload, "dateTime");
        String clientName = getStringPayload(payload, "clientName");

        // Notify the user (if authenticated appointment)
        if (userId != null) {
            sseNotificationService.notifyUserNewAppointment(userId, event.entityId(), subject, dateTime);
        }

        // Notify admins
        sseNotificationService.notifyAdminNewAppointment(event.entityId(), clientName, subject, dateTime);
        sseNotificationService.notifyDashboardUpdate();
    }

    private void handleAppointmentConfirmed(LmpBusinessEvent event) {
        Map<String, Object> payload = event.payload();
        String userId = getStringPayload(payload, "userId");
        String subject = getStringPayload(payload, "subject");
        String dateTime = getStringPayload(payload, "dateTime");

        if (userId != null) {
            sseNotificationService.notifyUserAppointmentStatusChanged(
                    userId, event.entityId(), subject, "EN ATTENTE", "CONFIRMÉ", dateTime);
        }

        sseNotificationService.notifyDashboardUpdate();
    }

    private void handleAppointmentCancelled(LmpBusinessEvent event) {
        Map<String, Object> payload = event.payload();
        String userId = getStringPayload(payload, "userId");
        String subject = getStringPayload(payload, "subject");
        String dateTime = getStringPayload(payload, "dateTime");
        String reason = getStringPayload(payload, "reason");

        if (userId != null) {
            sseNotificationService.notifyUserAppointmentCancelled(
                    userId, event.entityId(), subject, dateTime, reason);
        }

        sseNotificationService.notifyDashboardUpdate();
    }

    // ========== User Handlers ==========

    private void handleUserRegistered(LmpBusinessEvent event) {
        Map<String, Object> payload = event.payload();
        String email = getStringPayload(payload, "email");
        String displayName = getStringPayload(payload, "displayName");

        sseNotificationService.notifyAdminNewUser(event.entityId(), email, displayName);
        sseNotificationService.notifyDashboardUpdate();
    }

    // ========== Review Handlers ==========

    private void handleReviewCreated(LmpBusinessEvent event) {
        Map<String, Object> payload = event.payload();
        String clientName = getStringPayload(payload, "clientName");
        int rating = payload.containsKey("rating") ? ((Number) payload.get("rating")).intValue() : 0;

        sseNotificationService.notifyAdminNewReview(event.entityId(), clientName, rating);
        sseNotificationService.notifyDashboardUpdate();
    }

    // ========== Helpers ==========

    private String getStringPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
