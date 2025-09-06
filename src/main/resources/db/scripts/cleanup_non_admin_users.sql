-- Script de nettoyage des utilisateurs non-administrateurs
-- Ce script liste tous les utilisateurs et supprime (soft delete) tous ceux qui n'ont pas le rôle ADMIN

-- ============================================================================
-- ÉTAPE 1: LISTER TOUS LES UTILISATEURS ACTUELS AVEC LEURS RÔLES
-- ============================================================================

SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    u.status,
    u.account_locked,
    GROUP_CONCAT(r.name SEPARATOR ', ') as roles,
    u.registration_date
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
GROUP BY u.id, u.email, u.first_name, u.last_name, u.status, u.account_locked, u.registration_date
ORDER BY u.registration_date;

-- ============================================================================
-- ÉTAPE 2: IDENTIFIER LES UTILISATEURS QUI SERONT SUPPRIMÉS (NON-ADMIN)
-- ============================================================================

SELECT 
    'SERA SUPPRIMÉ' as action,
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    u.status,
    GROUP_CONCAT(r.name SEPARATOR ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id NOT IN (
    SELECT DISTINCT u2.id 
    FROM users u2
    JOIN user_roles ur2 ON u2.id = ur2.user_id
    JOIN roles r2 ON ur2.role_id = r2.id
    WHERE r2.name = 'ADMIN'
)
GROUP BY u.id
ORDER BY u.email;

-- ============================================================================
-- ÉTAPE 3: IDENTIFIER LES UTILISATEURS QUI SERONT CONSERVÉS (ADMIN)
-- ============================================================================

SELECT 
    'SERA CONSERVÉ' as action,
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    u.status,
    GROUP_CONCAT(r.name SEPARATOR ', ') as roles
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE r.name = 'ADMIN'
GROUP BY u.id
ORDER BY u.email;

-- ============================================================================
-- ÉTAPE 4: SUPPRIMER TOUS LES UTILISATEURS NON-ADMIN (SOFT DELETE)
-- ============================================================================

-- ATTENTION: Cette opération va marquer comme DELETED tous les utilisateurs 
-- qui n'ont pas le rôle ADMIN. Assurez-vous de bien avoir vérifié les étapes précédentes !

-- Décommentez les lignes suivantes pour exécuter la suppression :

/*
UPDATE users 
SET 
    status = 'DELETED',
    account_locked = TRUE
WHERE id NOT IN (
    SELECT DISTINCT u.id 
    FROM (SELECT DISTINCT u.id FROM users u
          JOIN user_roles ur ON u.id = ur.user_id
          JOIN roles r ON ur.role_id = r.id
          WHERE r.name = 'ADMIN') AS u
);
*/

-- ============================================================================
-- ÉTAPE 5: VÉRIFICATION POST-SUPPRESSION
-- ============================================================================

-- Décommentez pour vérifier le résultat après suppression :

/*
SELECT 
    u.status,
    COUNT(*) as nombre_utilisateurs,
    GROUP_CONCAT(DISTINCT u.email ORDER BY u.email SEPARATOR ', ') as emails
FROM users u
GROUP BY u.status
ORDER BY u.status;

SELECT 
    'Utilisateurs actifs restants' as info,
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    GROUP_CONCAT(r.name SEPARATOR ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.status = 'ACTIVE'
GROUP BY u.id
ORDER BY u.email;
*/

-- ============================================================================
-- NOTES IMPORTANTES:
-- ============================================================================
-- 1. Ce script utilise un SOFT DELETE (status = 'DELETED') pour préserver l'intégrité des données
-- 2. Les utilisateurs supprimés sont aussi verrouillés (account_locked = TRUE)
-- 3. Seuls les utilisateurs avec le rôle ADMIN sont conservés
-- 4. Vérifiez toujours les requêtes SELECT avant d'exécuter les UPDATE
-- 5. Il est recommandé de faire une sauvegarde avant d'exécuter ce script
-- ============================================================================
