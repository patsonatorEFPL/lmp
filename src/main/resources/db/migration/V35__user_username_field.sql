ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255) UNIQUE;

-- Migrer l'admin existant : assigner username=Administrator au premier admin trouvé
UPDATE users SET username = 'Administrator'
WHERE id = (
    SELECT u.id FROM users u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'ADMIN'
    ORDER BY u.registration_date ASC
    LIMIT 1
)
AND username IS NULL;
