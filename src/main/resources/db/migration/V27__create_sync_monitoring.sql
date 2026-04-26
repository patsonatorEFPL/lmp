-- =============================================================================
-- V27 : Observabilité & Alerting de la Synchronisation ERP
-- =============================================================================

-- ------------------------------------------------------------------------------
-- Table 1 : sync_health_snapshot
-- Snapshots périodiques de l'état de santé de la synchronisation.
-- Permet l'historisation et l'affichage de tendances (dashboard).
-- ------------------------------------------------------------------------------
CREATE TABLE sync_health_snapshot (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    -- État global synthétisé
    overall_status  VARCHAR(20) NOT NULL CHECK (overall_status IN ('HEALTHY', 'DEGRADED', 'CRITICAL')),

    -- Métriques dérivées de sync_event_log
    queue_depth     INTEGER NOT NULL DEFAULT 0,
    dead_count      INTEGER NOT NULL DEFAULT 0,
    failed_stale_count INTEGER NOT NULL DEFAULT 0,
    unverified_count   INTEGER NOT NULL DEFAULT 0,
    success_rate_24h   DECIMAL(5,4),

    -- État de l'ERP
    erp_available   BOOLEAN NOT NULL DEFAULT false,
    erp_latency_ms  INTEGER,

    -- Réconciliation
    reconciliation_gaps INTEGER NOT NULL DEFAULT 0,
    last_reconciliation_at TIMESTAMP,

    -- Patterns d'erreur nouveaux détectés lors de ce snapshot
    new_patterns_count INTEGER NOT NULL DEFAULT 0,

    -- Payload JSON brut pour extensibilité (métriques futures)
    raw_metrics     JSONB,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_health_snapshot_recorded
    ON sync_health_snapshot (recorded_at DESC);

CREATE INDEX idx_sync_health_snapshot_status
    ON sync_health_snapshot (overall_status, recorded_at DESC);

-- ------------------------------------------------------------------------------
-- Table 2 : sync_error_patterns
-- Catalogue des patterns d'erreur rencontrés lors de la synchronisation.
-- Permet la classification (KNOWN vs UNKNOWN) et la détection de drift.
-- ------------------------------------------------------------------------------
CREATE TABLE sync_error_patterns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Pattern : chaîne littère ou préfixe identifiant l'erreur
    pattern         VARCHAR(500) NOT NULL,

    -- Catégorie fonctionnelle
    category        VARCHAR(30) NOT NULL CHECK (category IN (
        'KNOWN_RECOVERABLE',   -- retryable : timeout, 503, TimestampMismatch
        'KNOWN_PERMANENT',     -- non-retryable : LinkValidationError, PermissionError
        'UNKNOWN'              -- jamais vu = drift potentiel
    )),

    -- Première occurrence
    first_seen_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Nombre total d'occurrences
    occurrences     INTEGER NOT NULL DEFAULT 1,

    -- Dernier event concerné (traçabilité)
    last_sync_event_id UUID,

    -- Flag : a déjà déclenché une alerte admin ?
    alerted         BOOLEAN NOT NULL DEFAULT false,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()

    -- Unicité sur le pattern (case-insensitive) gérée via index ci-dessous
);

CREATE UNIQUE INDEX idx_sync_error_pattern_unique
    ON sync_error_patterns (LOWER(pattern));

CREATE INDEX idx_sync_error_patterns_category
    ON sync_error_patterns (category, last_seen_at DESC);

CREATE INDEX idx_sync_error_patterns_alerted
    ON sync_error_patterns (alerted, category)
    WHERE alerted = false;
