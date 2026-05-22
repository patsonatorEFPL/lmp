package com.lmp.notification.mail.queue;

/**
 * Statut d'un email dans la queue.
 *
 * <pre>
 *   NOT_SENT --(claim)--> SENDING --(success)--> SENT
 *                              `--(error, retry < max)--> NOT_SENT (with backoff)
 *                              `--(error, retry >= max)--> ERROR
 *   *        --(send_after &lt; NOW() never reached + TTL)--> EXPIRED
 * </pre>
 */
public enum EmailQueueStatus {
    /** En attente — éligible au prochain claim worker. */
    NOT_SENT,
    /** Claimé atomiquement par un worker — SMTP send en cours. */
    SENDING,
    /** SMTP delivery confirmée. */
    SENT,
    /** Max retries atteint sans succès — terminal. */
    ERROR,
    /** Retired sans envoi (TTL exceeded, e.g. send_after > 30 days). */
    EXPIRED
}
