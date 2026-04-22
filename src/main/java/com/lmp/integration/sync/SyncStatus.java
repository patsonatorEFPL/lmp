package com.lmp.integration.sync;

/**
 * Statut d'un événement de synchronisation.
 */
public enum SyncStatus {
    /** En attente dans la queue — prêt à être traité. */
    QUEUED,
    /** En cours de traitement par le processor. */
    PROCESSING,
    /** Ancien statut — conservé pour compatibilité. */
    PENDING,
    /** Traitement réussi. */
    SUCCESS,
    /** Traitement échoué — retryable si retry_count < max_retries. */
    FAILED,
    /** Échec définitif — max retries atteint. */
    DEAD,
    /** Ignoré volontairement. */
    SKIPPED
}
