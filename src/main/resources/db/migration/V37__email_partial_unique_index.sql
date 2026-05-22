-- Replace blanket UNIQUE on email with a partial index.
-- NULLs (system accounts like Administrator) are exempt; all non-NULL emails must be unique.
-- This is the PostgreSQL-idiomatic equivalent of external CRM's STANDARD_USERS bypass.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
DROP INDEX IF EXISTS users_email_key;
CREATE UNIQUE INDEX users_email_unique_notnull ON users (email) WHERE email IS NOT NULL;
