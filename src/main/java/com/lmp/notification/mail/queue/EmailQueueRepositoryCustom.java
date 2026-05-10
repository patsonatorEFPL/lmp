package com.lmp.notification.mail.queue;

import java.util.List;

public interface EmailQueueRepositoryCustom {

    /**
     * Claim atomique du prochain batch d'emails à envoyer.
     * <p>
     * Sélectionne les rows {@code NOT_SENT} dont {@code send_after &lt;= NOW()},
     * triés par priority DESC, retry ASC, created_at ASC, applique
     * {@code FOR UPDATE SKIP LOCKED} pour multi-replica safety, puis flip
     * status à {@code SENDING} et retourne les rows mises à jour via
     * {@code RETURNING *} — opération atomique (pas de race read-then-write).
     */
    List<EmailQueueEvent> claimNextBatch(int batchSize);
}
