-- =============================================
-- V30: STAFF role + external ERP User external link
-- =============================================
-- Permet de distinguer les collaborateurs (sync external ERP "User" doctype)
-- des clients standards (sync external ERP "Customer" doctype).

INSERT INTO roles (name) VALUES ('STAFF') ON CONFLICT (name) DO NOTHING;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS external_erp_user_id VARCHAR(140);

CREATE INDEX IF NOT EXISTS idx_users_external_erp_user_id
    ON users(external_erp_user_id)
    WHERE external_erp_user_id IS NOT NULL;
