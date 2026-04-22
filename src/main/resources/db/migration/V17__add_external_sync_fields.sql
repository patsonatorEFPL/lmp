-- ============================================================================
-- V17 — Champs de liaison vers le système externe (agnostique ERP)
-- ============================================================================
-- Ajoute les identifiants externes sur les entités LMP existantes pour
-- établir le lien bidirectionnel avec un système externe quelconque.

ALTER TABLE users ADD COLUMN IF NOT EXISTS external_customer_id VARCHAR(140);
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_contact_id VARCHAR(140);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS external_order_id VARCHAR(140);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS external_invoice_id VARCHAR(140);

ALTER TABLE services ADD COLUMN IF NOT EXISTS external_item_code VARCHAR(140);

ALTER TABLE service_categories ADD COLUMN IF NOT EXISTS external_group_id VARCHAR(140);

-- Index partiels pour les lookups par ID externe (NULL exclu → index compact)
CREATE INDEX IF NOT EXISTS idx_users_external_customer ON users(external_customer_id) WHERE external_customer_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orders_external_order ON orders(external_order_id) WHERE external_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_services_external_item ON services(external_item_code) WHERE external_item_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_categories_external_group ON service_categories(external_group_id) WHERE external_group_id IS NOT NULL;
