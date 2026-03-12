package com.lmp.integration.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener central pour les événements métier LMP → external ERP.
 *
 * Architecture :
 *   Module (auth, billing, crm…) → ApplicationEventPublisher → LmpBusinessEvent → external ERPEventListener
 *
 * Ce listener est le SEUL point de contact avec external ERP.
 * Aucun autre module ne doit connaître external ERP directement.
 *
 * Futur : Ce listener transmettra les événements vers external ERP via n8n webhooks
 * ou directement via l'API REST external ERP.
 */
@Component
public class external ERPEventListener {

    private static final Logger logger = LoggerFactory.getLogger(external ERPEventListener.class);

    @Async
    @EventListener
    public void handleBusinessEvent(LmpBusinessEvent event) {
        logger.info("📡 [EVENT BUS] {} — module={}, entityId={}, eventId={}",
                event.type(), event.sourceModule(), event.entityId(), event.eventId());

        // TODO: Phase 2 — Transmettre vers external ERP via n8n ou API REST directe
        // Les URLs external ERP seront configurées via variables d'environnement
        // Exemples :
        //   ORDER_CONFIRMED  → POST ${external ERP_URL}/api/resource/Sales Order
        //   USER_REGISTERED  → POST ${external ERP_URL}/api/resource/Customer
        //   INVOICE_GENERATED → POST ${external ERP_URL}/api/resource/Sales Invoice

        switch (event.type()) {
            case USER_REGISTERED -> logEvent("Nouveau client à créer dans external ERP", event);
            case ORDER_CONFIRMED -> logEvent("Commande à synchroniser avec external ERP Sales Order", event);
            case PAYMENT_RECEIVED -> logEvent("Paiement à enregistrer dans external ERP Payment Entry", event);
            case INVOICE_GENERATED -> logEvent("Facture à créer dans external ERP Sales Invoice", event);
            case APPOINTMENT_CREATED -> logEvent("Rendez-vous à planifier dans external ERP Event", event);
            case CONTACT_FORM_SUBMITTED -> logEvent("Lead à créer dans external ERP CRM", event);
            default -> logger.debug("📡 [EVENT BUS] Événement non traité pour external ERP: {}", event.type());
        }
    }

    private void logEvent(String description, LmpBusinessEvent event) {
        logger.info("📡 [external ERP SYNC — STUB] {} | payload={}", description, event.payload());
    }
}
