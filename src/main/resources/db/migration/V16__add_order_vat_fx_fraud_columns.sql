-- V16: Add VAT snapshot, FX snapshot, and fraud scoring columns to orders table
-- These columns were auto-created by Hibernate ddl-auto=update in dev,
-- but must be explicitly migrated for staging/production (ddl-auto=validate).

-- VAT snapshot
ALTER TABLE orders ADD COLUMN IF NOT EXISTS applied_vat_rate NUMERIC(5, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS amount_base_eur NUMERIC(10, 2);

-- FX snapshot
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_rate NUMERIC(18, 6);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_source VARCHAR(32);

-- Guest checkout token
ALTER TABLE orders ADD COLUMN IF NOT EXISTS checkout_token VARCHAR(64);

-- Fraud scoring
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ip_country VARCHAR(2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vpn_score NUMERIC(5, 3);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vpn_sources VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS browser_timezone VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS geo_country VARCHAR(2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS card_country VARCHAR(2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fraud_score INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fraud_flags TEXT;
