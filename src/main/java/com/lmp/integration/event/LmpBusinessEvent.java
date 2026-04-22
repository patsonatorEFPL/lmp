package com.lmp.integration.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Événement métier générique LMP.
 *
 * Tous les modules publient ces événements via {@code ApplicationEventPublisher}.
 * Le module Integration les consomme et les transmet vers l’ERP (futur).
 *
 * Couplage faible : les modules émetteurs ne connaissent ni un ERP précis, ni n8n.
 * Un junior peut ajouter un nouveau type d'événement simplement en ajoutant une valeur à l'enum.
 */
public record LmpBusinessEvent(
        UUID eventId,
        EventType type,
        String sourceModule,
        UUID entityId,
        Map<String, Object> payload,
        LocalDateTime timestamp
) {

    /**
     * Types d'événements métier supportés.
     * Ajouter un nouveau type ici quand un nouveau flux vers l’ERP est nécessaire.
     */
    public enum EventType {
        // Auth
        USER_REGISTERED,
        USER_VERIFIED,
        USER_UPDATED,
        USER_DELETED,

        // Billing
        ORDER_CREATED,
        ORDER_CONFIRMED,
        ORDER_CANCELLED,
        /** Changement de statut / annulation admin (hors webhook seul). */
        ORDER_UPDATED,
        ORDER_DELETED,
        PAYMENT_RECEIVED,
        PAYMENT_FAILED,
        INVOICE_GENERATED,
        REFUND_PROCESSED,
        REFUND_STATUS_UPDATED,

        // CRM
        APPOINTMENT_CREATED,
        APPOINTMENT_CONFIRMED,
        APPOINTMENT_CANCELLED,
        APPOINTMENT_UPDATED,
        APPOINTMENT_DELETED,

        // Reviews (quand le flux métier publiera l'événement)
        REVIEW_CREATED,
        REVIEW_UPDATED,

        // Notification
        CONTACT_FORM_SUBMITTED,

        // Catalog
        SERVICE_CREATED,
        SERVICE_UPDATED,
        SERVICE_DELETED,

        // Projects
        PROJECT_CREATED,
        PROJECT_UPDATED,
        PROJECT_DELETED,
        TASK_CREATED,
        TASK_UPDATED,
        TASK_DELETED,

        // Support / Tickets
        TICKET_CREATED,
        TICKET_UPDATED,
        TICKET_RESOLVED,
        TICKET_DELETED
    }

    /**
     * Factory method — facilite la création d'événements.
     */
    public static LmpBusinessEvent of(EventType type, String sourceModule, UUID entityId, Map<String, Object> payload) {
        return new LmpBusinessEvent(
                UUID.randomUUID(),
                type,
                sourceModule,
                entityId,
                payload,
                LocalDateTime.now()
        );
    }
}
