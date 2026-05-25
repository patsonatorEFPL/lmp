-- SECURITY (M8) : persiste la clé de signature OAuth2 JWK en DB pour survivre
-- aux restarts pod + permettre scale-out multi-replica sans key drift.
--
-- Précédence dans AuthorizationServerConfig.jwkSource() :
--   1. env app.oauth2.jwk.content (12-factor, immutable, multi-replica safe)
--   2. table oauth2_signing_key WHERE active=true (cette migration)
--   3. fichier disque legacy
--   4. génération at-boot (last resort, warn)
--
-- Rotation : insert nouvelle row avec active=true + flip ancienne à false.
-- JWKSet expose toutes les clés actives → consumers peuvent valider tokens
-- signés par toute clé active (rotation sans invalider sessions en cours).

CREATE TABLE IF NOT EXISTS oauth2_signing_key (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_id       VARCHAR(64) NOT NULL UNIQUE,
    jwk_json     TEXT NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rotated_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_oauth2_signing_key_active
    ON oauth2_signing_key (active)
    WHERE active = true;

COMMENT ON TABLE oauth2_signing_key IS
    'JWK signing keys persisted for multi-replica OAuth2 authorization server (M8 fix). See AuthorizationServerConfig.jwkSource().';
COMMENT ON COLUMN oauth2_signing_key.jwk_json IS
    'Full JWK JSON including private key material (alg, d, dp, dq, e, kid, kty, n, p, q, qi, use). Treat as secret.';
COMMENT ON COLUMN oauth2_signing_key.active IS
    'true = exposed in JWKSet for both signing (newest active) and validation. Set false during rotation to retire a key from signing while keeping it for validation grace period — then DELETE row when grace expires.';
