package com.lmp.domain.enums;

public enum OrderStatus {
    PAYMENT_PENDING,  // En attente de paiement Stripe
    PENDING,          // En attente de traitement
    CONFIRMED,        // Confirmée
    PROCESSING,       // En cours de traitement (synonyme IN_PROGRESS)
    IN_PROGRESS,      // En cours de traitement
    SHIPPED,          // Expédiée
    DELIVERED,        // Livrée
    COMPLETED,        // Terminée
    UNDER_REVIEW,     // En révision
    CANCELLED,        // Annulée
    REFUNDED          // Remboursée
}
