package com.lmp.billing.domain;

/**
 * Aligne {@link Order#getProgressPercentage()} et {@link Order#getProgressStatus()} sur
 * {@link Order#getStatus()}, selon les mêmes seuils que le front (user-orders, dashboard, admin-orders).
 */
public final class OrderProgressSync {

    private OrderProgressSync() {}

    /**
     * Met à jour la progression pour refléter le statut actuel : plancher par statut, cas particuliers
     * annulation / remboursement, libellé par défaut si la progression était vide.
     */
    public static void applyMinimumForStatus(Order order) {
        if (order == null || order.getStatus() == null) {
            return;
        }
        switch (order.getStatus()) {
            case CANCELLED -> applyCancelled(order);
            case REFUNDED -> applyRefunded(order);
            case PAYMENT_PENDING, PENDING -> applyEarlyPipeline(order);
            case CONFIRMED -> applyFloored(order, 10, "Paiement confirmé");
            case UNDER_REVIEW -> applyFloored(order, 20, "En révision");
            case PROCESSING -> applyFloored(order, 30, "En traitement");
            case IN_PROGRESS -> applyFloored(order, 50, "En cours");
            case SHIPPED -> applyFloored(order, 80, "Livraison");
            case DELIVERED -> applyFloored(order, 90, "Livrée");
            case COMPLETED -> applyFloored(order, 100, "Terminée");
        }
    }

    private static void applyCancelled(Order order) {
        order.setProgressPercentage(0);
        order.setProgressStatus("Annulée");
    }

    private static void applyRefunded(Order order) {
        order.setProgressStatus("Remboursée");
    }

    private static void applyEarlyPipeline(Order order) {
        order.setProgressPercentage(0);
        order.setProgressStatus("Commande reçue");
    }

    private static void applyFloored(Order order, int floor, String label) {
        int cur = order.getProgressPercentage() != null ? order.getProgressPercentage() : 0;
        if (cur < floor) {
            order.setProgressPercentage(floor);
            order.setProgressStatus(label);
        } else {
            String ps = order.getProgressStatus();
            if (ps == null || ps.isBlank()) {
                order.setProgressStatus(label);
            }
        }
    }
}
