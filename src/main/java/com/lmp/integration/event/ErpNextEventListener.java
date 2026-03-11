package com.lmp.integration.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener central pour les événements métier LMP → ERPNext.
 *
 * Architecture :
 *   Module (auth, billing, crm…) → ApplicationEventPublisher → LmpBusinessEvent → ErpNextEventListener
 *
 * Ce listener est le SEUL point de contact avec ERPNext.
 * Aucun autre module ne doit connaître ERPNext directement.
 *
 * Futur : Ce listener transmettra les événements vers ERPNext via n8n webhooks
 * ou directement via l'API REST ERPNext.
 */
@Component
public class ErpNextEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ErpNextEventListener.class);

    @Async
    @EventListener
    public void handleBusinessEvent(LmpBusinessEvent event) {
        logger.info("📡 [EVENT BUS] {} — module={}, entityId={}, eventId={}",
                event.type(), event.sourceModule(), event.entityId(), event.eventId());

        // TODO: Phase 2 — Transmettre vers ERPNext via n8n ou API REST directe
        // Exemples :
        //   ORDER_CONFIRMED  → POST https://erpnext.lmp-services.ca/api/resource/Sales Order
        //   USER_REGISTERED  → POST https://erpnext.lmp-services.ca/api/resource/Customer
        //   INVOICE_GENERATED → POST https://erpnext.lmp-services.ca/api/resource/Sales Invoice

        switch (event.type()) {
            case USER_REGISTERED -> logEvent("Nouveau client à créer dans ERPNext", event);
            case ORDER_CONFIRMED -> logEvent("Commande à synchroniser avec ERPNext Sales Order", event);
            case PAYMENT_RECEIVED -> logEvent("Paiement à enregistrer dans ERPNext Payment Entry", event);
            case INVOICE_GENERATED -> logEvent("Facture à créer dans ERPNext Sales Invoice", event);
            case APPOINTMENT_CREATED -> logEvent("Rendez-vous à planifier dans ERPNext Event", event);
            case CONTACT_FORM_SUBMITTED -> logEvent("Lead à créer dans ERPNext CRM", event);
            default -> logger.debug("📡 [EVENT BUS] Événement non traité pour ERPNext: {}", event.type());
        }
    }

    private void logEvent(String description, LmpBusinessEvent event) {
        logger.info("📡 [ERPNext SYNC — STUB] {} | payload={}", description, event.payload());
    }
}
