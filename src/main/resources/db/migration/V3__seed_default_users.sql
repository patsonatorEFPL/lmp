-- =============================================
-- V3: Seed default roles and admin user
-- =============================================
-- Le hash du mot de passe administrateur initial est défini ci-dessous (BCrypt, cost 12).
-- IMPORTANT: Changez ce mot de passe immédiatement après la première connexion !
-- Le mot de passe initial de développement est documenté dans le README.md (section "Premier démarrage").
-- Insert default roles
INSERT INTO roles (name) VALUES ('USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;

-- Insert admin user
INSERT INTO users (email, password, first_name, last_name, registration_date, status, email_verified)
VALUES (
    'admin@lmp.ca',
    '$2b$12$xZgW8rkyBuf1ReSNaX845uulGVtODVzFg5T7BQ10tyQhSnIkcvBP6',
    'Admin',
    'LMP',
    NOW(),
    'ACTIVE',
    true
) ON CONFLICT (email) DO NOTHING;

-- Assign ADMIN + USER roles to admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@lmp.ca' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@lmp.ca' AND r.name = 'USER'
ON CONFLICT DO NOTHING;
