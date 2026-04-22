-- ============================================================================
-- V18 — Journal de synchronisation (sync_event_log)
-- ============================================================================
-- Trace chaque opération de synchronisation (entrante ou sortante).
-- Permet l'audit, le debugging, le retry automatique et la réconciliation.

CREATE TABLE IF NOT EXISTS sync_event_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    direction           VARCHAR(10)  NOT NULL CHECK (direction IN ('OUTBOUND', 'INBOUND')),
    entity_type         VARCHAR(50)  NOT NULL,
    local_entity_id     UUID,
    external_entity_id  VARCHAR(140),
    event_type          VARCHAR(50)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payload             TEXT,
    error_message       TEXT,
    retry_count         INTEGER      DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sync_status_direction ON sync_event_log(status, direction);
CREATE INDEX IF NOT EXISTS idx_sync_entity ON sync_event_log(entity_type, local_entity_id);
CREATE INDEX IF NOT EXISTS idx_sync_created ON sync_event_log(created_at);
