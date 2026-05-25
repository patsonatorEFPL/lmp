-- SECURITY (M10) : activer PKCE sur tous les clients OAuth2 enregistrés.
--
-- Le bean AuthorizationServerConfig builds RegisteredClient avec
-- requireProofKey(true) côté code, MAIS la branche `repository.save(erpClient)`
-- ne s'exécute que si le client n'existe pas déjà en DB. Pour les clients
-- déjà persistés (frappe-erp-client + autres), les ClientSettings côté Java
-- ne sont JAMAIS rejoués → require-proof-key reste à false en base.
--
-- Cette migration patch directement le JSON client_settings stocké par
-- JdbcRegisteredClientRepository : flip "settings.client.require-proof-key"
-- de false à true pour tous les clients.
--
-- Effet : prochain authorization request DOIT inclure code_challenge.
-- Si Frappe ne supporte pas, le flow casse → rollback via UPDATE inverse OU
-- recreate le client avec requireProofKey(false).
--
-- Rollback :
--   UPDATE oauth2_registered_client
--   SET client_settings = REPLACE(client_settings,
--       '"settings.client.require-proof-key":true',
--       '"settings.client.require-proof-key":false');

UPDATE oauth2_registered_client
SET client_settings = REPLACE(
    client_settings,
    '"settings.client.require-proof-key":false',
    '"settings.client.require-proof-key":true'
)
WHERE client_settings LIKE '%"settings.client.require-proof-key":false%';
