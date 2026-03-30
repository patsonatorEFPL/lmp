package com.lmp.integration.event;

import com.lmp.integration.event.LmpBusinessEvent.EventType;

/**
 * Noms d’événements canoniques pour le canal SSE (alignés avec le front : {@code order:}, {@code appointment:}, …).
 */
public final class BusinessEventSseNames {

    private BusinessEventSseNames() {
    }

    public static String canonicalSseEvent(EventType type) {
        return switch (type) {
            case ORDER_CREATED -> "order:created";
            case ORDER_UPDATED, ORDER_CONFIRMED, ORDER_CANCELLED -> "order:updated";
            case PAYMENT_RECEIVED -> "payment:succeeded";
            case PAYMENT_FAILED -> "payment:failed";
            case REFUND_PROCESSED -> "refund:created";
            case REFUND_STATUS_UPDATED -> "refund:updated";
            case INVOICE_GENERATED -> "invoice:created";
            case USER_REGISTERED -> "user:registered";
            case USER_VERIFIED, USER_UPDATED -> "user:updated";
            case APPOINTMENT_CREATED -> "appointment:created";
            case APPOINTMENT_CONFIRMED, APPOINTMENT_CANCELLED, APPOINTMENT_UPDATED -> "appointment:updated";
            case APPOINTMENT_DELETED -> "appointment:deleted";
            case REVIEW_CREATED -> "review:created";
            case REVIEW_UPDATED -> "review:updated";
            default -> "notification:created";
        };
    }
}
