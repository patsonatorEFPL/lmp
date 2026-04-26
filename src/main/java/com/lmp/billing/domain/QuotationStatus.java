package com.lmp.billing.domain;

/**
 * Statuts du cycle de vie d'un devis LMP.
 * Mappés vers les statuts ERPNext Quotation (Draft/Open/Ordered/Lost/Expired).
 */
public enum QuotationStatus {
    /** Brouillon — pas encore envoyé au client. */
    DRAFT,
    /** Envoyé au client — en attente de réponse. */
    SENT,
    /** Accepté par le client — prêt à être converti en commande. */
    ACCEPTED,
    /** Refusé par le client. */
    REJECTED,
    /** Expiré — validUntil dépassé sans réponse. */
    EXPIRED
}
