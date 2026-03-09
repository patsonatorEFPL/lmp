-- Insertion des rôles par défaut
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('ADMIN');

-- Insertion d'un utilisateur administrateur par défaut (mot de passe: admin123)
-- Le mot de passe est hashé avec BCrypt
INSERT INTO users (
    email, 
    password, 
    first_name, 
    last_name, 
    registration_date, 
    status, 
    account_locked, 
    email_verified
) VALUES (
    'admin@lmp.ca',
    '$2a$10$JvKv5pJEiNZECiFQgUnv8O8WY8Uho9pX5p.7gO3QX8pZ8mL0uG/2G',
    'Admin',
    'LMP',
    NOW(),
    'ACTIVE',
    FALSE,
    TRUE
);

-- Attribuer le rôle ADMIN à l'utilisateur administrateur
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'admin@lmp.ca' AND r.name = 'ADMIN';

-- Insertion d'un utilisateur test (mot de passe: user123)
INSERT INTO users (
    email, 
    password, 
    first_name, 
    last_name, 
    registration_date, 
    status, 
    account_locked, 
    email_verified
) VALUES (
    'user@lmp.ca',
    '$2a$10$4eDz4HhEpk.9KwXE3YVPte2x0r0xWcJgJmQG9Xd4nV7zFpQdnK/ni',
    'User',
    'Test',
    NOW(),
    'ACTIVE',
    FALSE,
    TRUE
);

-- Attribuer le rôle USER à l'utilisateur test
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'user@lmp.ca' AND r.name = 'USER';