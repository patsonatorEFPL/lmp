-- Lien sécurisé pour commandes « invité » (sans utilisateur) : paiement après inscription via token unique.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS checkout_token VARCHAR(64) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_checkout_token ON orders (checkout_token) WHERE checkout_token IS NOT NULL;
