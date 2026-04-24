-- V24 — Ajouter external_payment_id à la table orders (manquant dans V17)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS external_payment_id VARCHAR(140);

CREATE INDEX IF NOT EXISTS idx_orders_external_payment ON orders(external_payment_id) WHERE external_payment_id IS NOT NULL;
