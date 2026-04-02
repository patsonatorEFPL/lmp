-- Préférences TVA client (autoliquidation / auto-reverse) et snapshot sur commande pour la facture

ALTER TABLE users
    ADD COLUMN vat_reverse_charge BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN vat_number VARCHAR(64);

ALTER TABLE orders
    ADD COLUMN vat_reverse_charge BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE orders
    ADD COLUMN customer_vat_number VARCHAR(64);
