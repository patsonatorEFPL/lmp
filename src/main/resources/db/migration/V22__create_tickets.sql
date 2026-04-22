-- ============================================================================
-- V22 — Table Tickets (support / issues)
-- ============================================================================
-- Tickets de support synchronisables avec le système externe.
-- Visibles dans le dashboard client LMP.

CREATE TABLE IF NOT EXISTS tickets (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject               VARCHAR(255) NOT NULL,
    description           TEXT,
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    priority              VARCHAR(20)  DEFAULT 'MEDIUM',
    ticket_type           VARCHAR(50),
    resolution_details    TEXT,
    customer_id           UUID         REFERENCES users(id),
    external_issue_id     VARCHAR(140),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    resolved_at           TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_customer ON tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_external ON tickets(external_issue_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
