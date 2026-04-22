-- ============================================================================
-- V20 — Enrichir sync_event_log pour fonctionner comme queue FIFO
-- ============================================================================
-- Ajoute les colonnes nécessaires au traitement FIFO avec retry et backoff.
-- La table sync_event_log sert désormais de log ET de queue d'exécution.

ALTER TABLE sync_event_log
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS max_retries  INTEGER   DEFAULT 5,
    ADD COLUMN IF NOT EXISTS operation    VARCHAR(20) DEFAULT 'CREATE';

-- Mettre à jour les enregistrements existants
UPDATE sync_event_log SET scheduled_at = created_at WHERE scheduled_at IS NULL;

-- Index FIFO pour le processor (status QUEUED/PENDING, trié par scheduled_at puis created_at)
CREATE INDEX IF NOT EXISTS idx_sync_queue_fifo
    ON sync_event_log(status, scheduled_at, created_at)
    WHERE status IN ('QUEUED', 'PENDING');

-- Index pour le retry scheduler (status FAILED, retry_count < max)
CREATE INDEX IF NOT EXISTS idx_sync_retry_candidates
    ON sync_event_log(status, retry_count, scheduled_at)
    WHERE status = 'FAILED';
