-- ============================================================================
-- V26 — Création des tables quotations et quotation_items
-- ============================================================================
-- Devis LMP : cycle DRAFT → SENT → ACCEPTED/REJECTED/EXPIRED.
-- Un devis ACCEPTED peut être converti en commande (converted_order_id).

CREATE TABLE IF NOT EXISTS quotations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    title                   VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_amount            NUMERIC(10,2) NOT NULL,
    applied_vat_rate        NUMERIC(6,4),
    vat_reverse_charge      BOOLEAN DEFAULT FALSE,
    currency                VARCHAR(3) DEFAULT 'EUR',
    valid_until             TIMESTAMP,
    accepted_at             TIMESTAMP,
    billing_name            VARCHAR(255),
    admin_notes             TEXT,
    notes                   TEXT,
    external_quotation_id   VARCHAR(140),
    converted_order_id      UUID REFERENCES orders(id),
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS quotation_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_id    UUID NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    service_id      UUID NOT NULL REFERENCES services(id),
    quantity        INTEGER DEFAULT 1,
    price           NUMERIC(10,2) NOT NULL,
    description     TEXT
);

-- Index pour les requêtes courantes
CREATE INDEX IF NOT EXISTS idx_quotations_user ON quotations(user_id);
CREATE INDEX IF NOT EXISTS idx_quotations_status ON quotations(status);
CREATE INDEX IF NOT EXISTS idx_quotations_external ON quotations(external_quotation_id)
    WHERE external_quotation_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_quotation_items_quotation ON quotation_items(quotation_id);
