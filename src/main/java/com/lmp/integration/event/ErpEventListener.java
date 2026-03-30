package com.lmp.integration.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener central pour les événements métier LMP → ERP (intégration sortante).
 *
 * Architecture :
 *   Module (auth, billing, crm…) → ApplicationEventPublisher → LmpBusinessEvent → ErpEventListener
 *
 * Ce listener est le point d’entrée unique pour pousser les événements vers l’ERP.
 * Les modules métier ne dépendent pas d’un fournisseur ERP précis.
 *
 * Futur : webhooks (ex. n8n), connecteur REST ou message queue selon l’ERP retenu.
 */
@Component
public class ErpEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ErpEventListener.class);

    @Async
    @EventListener
    public void handleBusinessEvent(LmpBusinessEvent event) {
        logger.info("📡 [EVENT BUS] {} — module={}, entityId={}, eventId={}",
                event.type(), event.sourceModule(), event.entityId(), event.eventId());

        // TODO: Phase 2 — Pousser vers l’ERP (URL / credentials via configuration, pas de nom de produit en dur)

        switch (event.type()) {
            case USER_REGISTERED, USER_VERIFIED, USER_UPDATED ->
                    logEvent("Utilisateur / client — synchronisation ERP", event);
            case ORDER_CREATED, ORDER_CONFIRMED, ORDER_UPDATED, ORDER_CANCELLED ->
                    logEvent("Commande — synchronisation ERP", event);
            case PAYMENT_RECEIVED, PAYMENT_FAILED ->
                    logEvent("Paiement — synchronisation ERP", event);
            case INVOICE_GENERATED -> logEvent("Facture — synchronisation ERP", event);
            case REFUND_PROCESSED, REFUND_STATUS_UPDATED ->
                    logEvent("Remboursement — synchronisation ERP", event);
            case APPOINTMENT_CREATED, APPOINTMENT_CONFIRMED, APPOINTMENT_CANCELLED,
                    APPOINTMENT_UPDATED, APPOINTMENT_DELETED ->
                    logEvent("Rendez-vous — synchronisation ERP", event);
            case REVIEW_CREATED, REVIEW_UPDATED -> logEvent("Avis — synchronisation ERP / CRM", event);
            case CONTACT_FORM_SUBMITTED -> logEvent("Lead — synchronisation ERP (CRM)", event);
            case SERVICE_CREATED, SERVICE_UPDATED -> logEvent("Catalogue service — synchronisation ERP", event);
            default -> logger.debug("📡 [EVENT BUS] Événement non routé vers l’ERP : {}", event.type());
        }
    }

    private void logEvent(String description, LmpBusinessEvent event) {
        logger.info("📡 [ERP SYNC — STUB] {} | payload={}", description, event.payload());
    }
}
