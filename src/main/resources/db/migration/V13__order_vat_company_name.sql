-- Nom de société lié au numéro de TVA (reverse charge).
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vat_company_name VARCHAR(255) NULL;
