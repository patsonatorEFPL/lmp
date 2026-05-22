-- Link Order to the ServiceOffer it was created from, for idempotency dedup
-- and to detect duplicate purchases of the same offer.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS service_offer_id UUID NULL;

CREATE INDEX IF NOT EXISTS idx_orders_user_offer_status
    ON orders(user_id, service_offer_id, status);
