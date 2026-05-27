-- SECURITY (M10) — RE-ENABLE PKCE : Frappe app `lmp_branding` patched to
-- emit code_challenge/code_challenge_method on authorize URL and pass
-- code_verifier on token exchange. Spring Authorization Server can now
-- enforce PKCE S256 on the external CRM client.
--
-- Frappe-side implementation (deployed on prod Frappe container) :
--   lmp_branding/__init__.py   — imports pkce_patch on app load
--   lmp_branding/pkce_patch.py — monkey-patches frappe.utils.oauth.get_oauth2_authorize_url
--                                 to add code_challenge + cache verifier in Redis
--   lmp_branding/sso.py        — login_via_keycloak reads verifier from cache via
--                                 state.token and injects it into token exchange
--
-- E2E validated 2026-05-27 : click SSO Frappe → /login LMP → submit creds →
-- /oauth2/authorize (with code_challenge enforced) → code → callback Frappe →
-- token exchange with code_verifier → /desk Administrator. PKCE S256 active.
--
-- Reverse path : if Frappe patch must be rolled back, run :
--   UPDATE oauth2_registered_client SET client_settings = REPLACE(
--     client_settings,
--     '"settings.client.require-proof-key":true',
--     '"settings.client.require-proof-key":false'
--   ) WHERE client_settings LIKE '%"settings.client.require-proof-key":true%';

UPDATE oauth2_registered_client
SET client_settings = REPLACE(
    client_settings,
    '"settings.client.require-proof-key":false',
    '"settings.client.require-proof-key":true'
)
WHERE client_settings LIKE '%"settings.client.require-proof-key":false%';
