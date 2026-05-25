-- HOTFIX (post-merge prod regression) : VARCHAR(100) trop court pour Argon2id.
--
-- L'auto-upgrade bcrypt → argon2id (commit 3b1fce0 — UserDetailsPasswordService)
-- échoue avec "value too long for type character varying(100)" :
--
--   - bcrypt {2a$10$...} = 60 chars
--   - argon2id {argon2id}$argon2id$v=19$m=12288,t=2,p=1$<salt-22>$<hash-43>
--     = environ 120-130 chars (variable selon salt + hash base64)
--
-- Chaque login d'un user au hash bcrypt legacy déclenche updatePassword()
-- → INSERT/UPDATE de l'argon2id encoded → DataIntegrityViolationException
-- → 500 côté API + login KO (mais session pré-créée avant l'erreur).
--
-- Fix : VARCHAR(255) couvre confortablement argon2id + future paramétrage
-- mémoire/iterations. TEXT serait OK aussi mais VARCHAR garde la borne
-- explicite côté schéma.

ALTER TABLE users
    ALTER COLUMN password TYPE VARCHAR(255);
