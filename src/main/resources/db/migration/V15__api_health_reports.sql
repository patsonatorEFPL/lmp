-- =============================================
-- V15 : Rapports quotidiens de santé API (rétention 7 jours)
-- =============================================

CREATE TABLE api_health_reports (
    id            BIGSERIAL       PRIMARY KEY,
    report_date   DATE            NOT NULL UNIQUE,
    report_data   JSONB           NOT NULL,
    generated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_reports_date ON api_health_reports(report_date);
