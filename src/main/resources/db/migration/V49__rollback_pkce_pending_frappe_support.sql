-- SECURITY (M10) — ROLLBACK PARTIEL : désactiver PKCE le temps que Frappe
-- supporte code_challenge côté client.
--
-- V48 a flippé require-proof-key=true sur tous les clients OAuth2, mais
-- Frappe utilise la lib `rauth.OAuth2Service` (voir frappe/utils/oauth.py:120)
-- qui n'envoie pas `code_challenge` sur /oauth2/authorize. Sans le challenge,
-- Spring Authorization Server refuse l'authorize avec invalid_request → SSO ERP
-- complètement cassé.
--
-- Plan de ré-activation PKCE :
--   1. Soit patcher Frappe pour ajouter code_verifier + code_challenge_method=S256
--      à l'authorize URL + transmettre code_verifier au token exchange (rauth
--      le permet via params=)
--   2. Soit migrer Frappe rauth → authlib (PKCE natif via
--      OAuth2Session(code_challenge_method='S256'))
--   3. Une fois Frappe émet PKCE, re-run V48 (idempotent) via SQL ad-hoc OU
--      nouvelle migration V50__reenable_pkce.sql
--
-- Le TTL reduction (4h → 1h) de M10 reste en place côté code (commit 732c798).

UPDATE oauth2_registered_client
SET client_settings = REPLACE(
    client_settings,
    '"settings.client.require-proof-key":true',
    '"settings.client.require-proof-key":false'
)
WHERE client_settings LIKE '%"settings.client.require-proof-key":true%';
