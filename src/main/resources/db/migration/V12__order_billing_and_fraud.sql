-- Nom de facturation personnalisé + champs anti-fraude / scoring TVA.

-- Billing name
ALTER TABLE orders ADD COLUMN IF NOT EXISTS billing_name        VARCHAR(255)  NULL;

-- Fraud scoring fields
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ip_country          VARCHAR(2)    NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ip_address          VARCHAR(45)   NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vpn_score           NUMERIC(5,3)  NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vpn_sources         VARCHAR(255)  NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS browser_timezone    VARCHAR(64)   NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS geo_country         VARCHAR(2)    NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS card_country        VARCHAR(2)    NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fraud_score         INTEGER       NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fraud_flags         TEXT          NULL;

-- VAT snapshot (reverse charge)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vat_reverse_charge  BOOLEAN       NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_vat_number VARCHAR(20)   NULL;
