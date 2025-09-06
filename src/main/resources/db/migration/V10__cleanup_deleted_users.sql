-- Migration pour supprimer définitivement les utilisateurs avec statut DELETED
-- Cette migration fait suite à l'abandon du soft delete au profit du hard delete

-- Étape 1: Identifier les utilisateurs à supprimer
-- (Cette étape est informative, les données seront loggées dans l'application)

-- Étape 2: Anonymiser les commandes des utilisateurs supprimés
UPDATE orders 
SET user_id = NULL, 
    notes = CONCAT('[Utilisateur supprimé] ', COALESCE(notes, ''))
WHERE user_id IN (
    SELECT id FROM users WHERE status = 'DELETED'
);

-- Étape 3: Anonymiser les avis des utilisateurs supprimés  
UPDATE reviews
SET user_id = NULL,
    comment = CONCAT('[Utilisateur supprimé] ', COALESCE(comment, ''))
WHERE user_id IN (
    SELECT id FROM users WHERE status = 'DELETED'  
);

-- Étape 4: Anonymiser les rendez-vous des utilisateurs supprimés
-- Première étape: copier les informations utilisateur dans les champs client
UPDATE appointments a
INNER JOIN users u ON a.user_id = u.id
SET 
    a.client_name = CONCAT('[Supprimé] ', COALESCE(CONCAT(u.first_name, ' ', u.last_name), 'Utilisateur inconnu')),
    a.client_email = COALESCE(u.email, 'email-supprime@example.com'),
    a.client_phone = COALESCE(u.phone, 'N/A'),
    a.admin_notes = CONCAT('[Utilisateur supprimé] ', COALESCE(a.admin_notes, ''))
WHERE u.status = 'DELETED';

-- Deuxième étape: anonymiser en mettant user_id à NULL
UPDATE appointments 
SET user_id = NULL
WHERE user_id IN (
    SELECT id FROM users WHERE status = 'DELETED'
);

-- Étape 5: Supprimer les rôles des utilisateurs supprimés
DELETE FROM user_roles 
WHERE user_id IN (
    SELECT id FROM users WHERE status = 'DELETED'
);

-- Étape 6: Supprimer définitivement les utilisateurs avec statut DELETED
DELETE FROM users WHERE status = 'DELETED';

-- Étape 7: Ajouter un commentaire informatif
-- Cette migration marque la transition du soft delete vers le hard delete
-- Tous les futurs appels à deleteUser() feront maintenant une suppression définitive
