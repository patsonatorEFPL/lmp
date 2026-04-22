-- ============================================================================
-- V19 — Configuration du système externe (external_system_config)
-- ============================================================================
-- Table singleton-like pour stocker la config runtime du système externe.
-- Les properties Spring (lmp.sync.*) ont priorité ; cette table sert de
-- stockage secondaire pour les feature flags pilotés depuis l'admin.

CREATE TABLE IF NOT EXISTS external_system_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    system_key      VARCHAR(50)  UNIQUE NOT NULL,
    display_name    VARCHAR(100),
    base_url        VARCHAR(500),
    enabled         BOOLEAN      DEFAULT false,
    sync_features   TEXT         DEFAULT '{}',
    last_sync_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

-- Config par défaut — désactivée
INSERT INTO external_system_config (system_key, display_name, enabled)
VALUES ('primary_erp', 'Système ERP principal', false)
ON CONFLICT (system_key) DO NOTHING;
