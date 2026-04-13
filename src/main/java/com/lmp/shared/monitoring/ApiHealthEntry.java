package com.lmp.shared.monitoring;

/**
 * DTO de santé d'une API externe.
 *
 * @param name         identifiant de l'API
 * @param status       UP, DEGRADED, DOWN ou UNKNOWN
 * @param avgLatencyMs latence moyenne sur la fenêtre glissante (ms)
 * @param successRate  taux de succès en % (ex: 98.5)
 * @param totalCalls   nombre total d'appels dans la fenêtre
 * @param lastCallAt   ISO timestamp du dernier appel (null si aucun)
 * @param lastError    dernier message d'erreur (null si aucun)
 */
public record ApiHealthEntry(
        String name,
        String status,
        long avgLatencyMs,
        double successRate,
        long totalCalls,
        String lastCallAt,
        String lastError
) {}
