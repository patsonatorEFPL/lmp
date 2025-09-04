package com.lmp.domain.enums;

/**
 * Énumération des statuts possibles pour un rendez-vous.
 * 
 * - PENDING: Rendez-vous en attente de confirmation
 * - CONFIRMED: Rendez-vous confirmé
 * - IN_PROGRESS: Rendez-vous en cours
 * - COMPLETED: Rendez-vous terminé
 * - CANCELLED: Rendez-vous annulé
 * - NO_SHOW: Client ne s'est pas présenté
 */
public enum AppointmentStatus {
    PENDING("En attente"),
    CONFIRMED("Confirmé"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminé"),
    CANCELLED("Annulé"),
    NO_SHOW("Absence");

    private final String displayName;

    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retourne true si le statut permet la modification du rendez-vous
     */
    public boolean isModifiable() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Retourne true si le statut permet l'annulation du rendez-vous
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Retourne true si le rendez-vous est actif (pas annulé ou terminé)
     */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED || this == IN_PROGRESS;
    }
}