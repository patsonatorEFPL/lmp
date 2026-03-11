package com.lmp.integration.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Événement métier générique LMP.
 *
 * Tous les modules publient ces événements via {@code ApplicationEventPublisher}.
 * Le module Integration les consomme et les transmet vers ERPNext (futur).
 *
 * Couplage faible : les modules émetteurs ne connaissent ni ERPNext, ni n8n.
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
     * Ajouter un nouveau type ici quand un nouveau flux ERPNext est nécessaire.
     */
    public enum EventType {
        // Auth
        USER_REGISTERED,
        USER_VERIFIED,

        // Billing
        ORDER_CREATED,
        ORDER_CONFIRMED,
        ORDER_CANCELLED,
        PAYMENT_RECEIVED,
        INVOICE_GENERATED,
        REFUND_PROCESSED,

        // CRM
        APPOINTMENT_CREATED,
        APPOINTMENT_CONFIRMED,
        APPOINTMENT_CANCELLED,

        // Notification
        CONTACT_FORM_SUBMITTED,

        // Catalog
        SERVICE_CREATED,
        SERVICE_UPDATED
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
