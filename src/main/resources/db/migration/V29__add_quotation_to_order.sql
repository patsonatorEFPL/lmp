-- Lien bidirectionnel Order ↔ Quotation (local LMP)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS quotation_id UUID NULL REFERENCES quotations(id);

CREATE INDEX IF NOT EXISTS idx_orders_quotation_id ON orders(quotation_id);
