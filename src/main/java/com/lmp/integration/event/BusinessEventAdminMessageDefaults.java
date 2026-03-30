package com.lmp.integration.event;

import java.util.Map;
import java.util.Objects;

import com.lmp.integration.event.LmpBusinessEvent.EventType;

/**
 * Libellés admin / toast unifié lorsque l’émetteur n’a pas fourni {@link BusinessEventPayloadKeys#MESSAGE}.
 * Les émetteurs sont encouragés à toujours renseigner le message ; ce bloc évite les toasts vides.
 */
public final class BusinessEventAdminMessageDefaults {

    private BusinessEventAdminMessageDefaults() {
    }

    public static void ensureAdminMessage(EventType type, Map<String, Object> payload) {
        if (payload.get(BusinessEventPayloadKeys.MESSAGE) != null) {
            return;
        }
        String msg = switch (type) {
            case ORDER_CREATED -> {
                String who = Objects.toString(payload.get(BusinessEventPayloadKeys.CUSTOMER_NAME), null);
                if (who == null || who.isEmpty()) {
                    who = Objects.toString(payload.get(BusinessEventPayloadKeys.CUSTOMER_EMAIL), "");
                }
                yield "Nouvelle commande — " + who;
            }
            case ORDER_UPDATED -> String.format("Commande %s : %s → %s",
                    Objects.toString(payload.get(BusinessEventPayloadKeys.ORDER_ID), "?"),
                    Objects.toString(payload.get(BusinessEventPayloadKeys.OLD_STATUS), "?"),
                    Objects.toString(payload.get(BusinessEventPayloadKeys.NEW_STATUS), "?"));
            case PAYMENT_RECEIVED -> String.format("Paiement confirmé pour la commande %s",
                    Objects.toString(payload.get(BusinessEventPayloadKeys.ORDER_ID), "?"));
            case PAYMENT_FAILED -> "Échec de paiement — "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.ERROR), "erreur inconnue");
            case REFUND_PROCESSED -> "Remboursement enregistré — commande "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.ORDER_ID), "?");
            case REFUND_STATUS_UPDATED -> "Mise à jour remboursement — "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.STRIPE_REFUND_ID), "");
            case INVOICE_GENERATED -> "Facture disponible — commande "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.ORDER_ID), "?");
            case USER_REGISTERED -> "Nouvel utilisateur : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.EMAIL), "");
            case USER_VERIFIED -> "Email vérifié : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.EMAIL), "");
            case USER_UPDATED -> "Profil mis à jour : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.EMAIL), "");
            case APPOINTMENT_CREATED -> "Nouveau RDV : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.CLIENT_NAME), "");
            case APPOINTMENT_CONFIRMED -> "RDV confirmé : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.SUBJECT), "");
            case APPOINTMENT_CANCELLED -> "RDV annulé : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.SUBJECT), "");
            case APPOINTMENT_UPDATED -> "RDV modifié : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.SUBJECT), "");
            case APPOINTMENT_DELETED -> "RDV supprimé : "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.SUBJECT), "");
            case REVIEW_CREATED, REVIEW_UPDATED -> "Nouvel avis / avis mis à jour";
            case ORDER_CONFIRMED -> "Commande confirmée — "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.ORDER_ID), "?");
            case ORDER_CANCELLED -> "Commande annulée — "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.ORDER_ID), "?");
            case CONTACT_FORM_SUBMITTED -> "Nouveau message contact — "
                    + Objects.toString(payload.get(BusinessEventPayloadKeys.EMAIL), "");
            case SERVICE_CREATED -> "Nouveau service catalogue";
            case SERVICE_UPDATED -> "Service catalogue mis à jour";
            default -> "Notification";
        };
        payload.put(BusinessEventPayloadKeys.MESSAGE, msg);
    }
}
