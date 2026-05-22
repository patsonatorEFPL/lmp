-- Ajoute external_address_id sur users pour la synchronisation external ERP Phase 3
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_address_id VARCHAR(140);

CREATE INDEX IF NOT EXISTS idx_users_external_address_id ON users(external_address_id);
