-- =============================================
-- V14 : Table de rétention des métriques API (24h glissantes)
-- =============================================

CREATE TABLE api_health_records (
    id          BIGSERIAL       PRIMARY KEY,
    api_name    VARCHAR(100)    NOT NULL,
    latency_ms  INTEGER         NOT NULL,
    success     BOOLEAN         NOT NULL,
    error       TEXT,
    recorded_at TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_records_api_time ON api_health_records(api_name, recorded_at);
CREATE INDEX idx_health_records_recorded_at ON api_health_records(recorded_at);
