package com.lmp.billing.domain;

/**
 * Statut d'une échéance de paiement.
 */
public enum InstallmentStatus {
    /** En attente de paiement. */
    PENDING,
    /** Paiement en cours de traitement (Stripe processing). */
    PROCESSING,
    /** Paiement confirmé. */
    PAID,
    /** Paiement échoué — relance nécessaire. */
    FAILED,
    /** Échéance annulée (commande annulée). */
    CANCELLED
}
