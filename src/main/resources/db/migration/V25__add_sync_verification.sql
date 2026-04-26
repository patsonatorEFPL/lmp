-- ============================================================================
-- V25 — Ajout de la vérification post-sync sur sync_event_log
-- ============================================================================
-- Permet de tracer quand un événement SUCCESS a été vérifié côté système externe.
-- Un event SUCCESS sans verified_at signifie que la vérification a échoué ou
-- n'a pas encore eu lieu. Le SyncReconciliationService signale les cas > 1h.

ALTER TABLE sync_event_log
    ADD COLUMN IF NOT EXISTS verified_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verification_error TEXT;

-- Index partiel pour trouver rapidement les events SUCCESS non vérifiés
CREATE INDEX IF NOT EXISTS idx_sync_unverified_success
    ON sync_event_log(status, processed_at)
    WHERE status = 'SUCCESS' AND verified_at IS NULL;
