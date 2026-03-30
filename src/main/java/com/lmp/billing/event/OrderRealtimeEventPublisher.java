package com.lmp.billing.event;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.lmp.auth.domain.User;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.integration.event.LmpBusinessEvent;

/**
 * Publie les événements métier liés au statut des commandes (bus Spring → SSE après commit).
 * Vit dans billing : connaît {@link Order} / {@link OrderStatus}, pas le module shared.
 */
@Component
public class OrderRealtimeEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OrderRealtimeEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Webhooks Stripe, synchro admin, réconciliation : passage à {@link OrderStatus#CONFIRMED} déclenche
     * {@link LmpBusinessEvent.EventType#PAYMENT_RECEIVED} ; les autres transitions → {@code ORDER_UPDATED}.
     */
    public void publishAutomatedStripeFlowTransition(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (order == null || oldStatus == newStatus) {
            return;
        }
        if (newStatus == OrderStatus.CONFIRMED) {
            publishPaymentReceived(order);
        } else {
            publishOrderUpdated(order, oldStatus, newStatus);
        }
    }

    /**
     * Toujours {@code ORDER_UPDATED} (PUT admin, annulation, remboursement total, etc.).
     */
    public void publishOrderUpdated(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (order == null || oldStatus == newStatus) {
            return;
        }
        Map<String, Object> pl = basePayload(order);
        pl.put(BusinessEventPayloadKeys.OLD_STATUS, oldStatus.name());
        pl.put(BusinessEventPayloadKeys.NEW_STATUS, newStatus.name());
        String line = String.format("Commande %s : %s → %s",
                order.getId(), oldStatus.name(), newStatus.name());
        pl.put(BusinessEventPayloadKeys.MESSAGE, line);
        pl.put(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE, line);
        if (order.getUser() != null) {
            pl.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.TRUE);
            pl.put(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE, "STATUS_CHANGED");
        }
        eventPublisher.publishEvent(LmpBusinessEvent.of(LmpBusinessEvent.EventType.ORDER_UPDATED, "billing",
                order.getId(), pl));
    }

    /** Paiement reçu (ex. commande créée déjà payée via webhook Stripe). */
    public void publishPaymentReceived(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        Map<String, Object> pl = basePayload(order);
        pl.put(BusinessEventPayloadKeys.MESSAGE, String.format("Paiement confirmé pour la commande %s",
                order.getId()));
        String svc = order.getServiceName() != null ? order.getServiceName() : "Commande";
        double amt = order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0d;
        pl.put(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE,
                String.format("Paiement confirmé : %s (%.2f€)", svc, amt));
        if (order.getUser() != null) {
            pl.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.TRUE);
            pl.put(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE, "PAYMENT_SUCCESS");
        }
        eventPublisher.publishEvent(LmpBusinessEvent.of(LmpBusinessEvent.EventType.PAYMENT_RECEIVED, "billing",
                order.getId(), pl));
    }

    private static Map<String, Object> basePayload(Order order) {
        Map<String, Object> pl = new HashMap<>();
        pl.put(BusinessEventPayloadKeys.ORDER_ID, order.getId().toString());
        User user = order.getUser();
        if (user != null) {
            pl.put(BusinessEventPayloadKeys.USER_ID, user.getId().toString());
        }
        String customerName = "";
        if (user != null) {
            String fn = user.getFirstName() != null ? user.getFirstName() : "";
            String ln = user.getLastName() != null ? user.getLastName() : "";
            customerName = (fn + " " + ln).trim();
            if (customerName.isEmpty()) {
                customerName = user.getEmail() != null ? user.getEmail() : "";
            }
        }
        pl.put(BusinessEventPayloadKeys.CUSTOMER_NAME, customerName);
        pl.put(BusinessEventPayloadKeys.SERVICE_NAME, order.getServiceName());
        if (order.getTotalAmount() != null) {
            pl.put(BusinessEventPayloadKeys.AMOUNT, order.getTotalAmount().doubleValue());
        }
        return pl;
    }
}
