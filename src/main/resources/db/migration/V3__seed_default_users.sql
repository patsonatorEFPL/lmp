-- =============================================
-- V3: Seed default roles and admin user
-- =============================================
-- The default admin password hash is seeded below.
-- IMPORTANT: Please change the admin password immediately after your first login!
-- Insert default roles
INSERT INTO roles (name) VALUES ('USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;

-- Insert admin user
INSERT INTO users (email, password, first_name, last_name, registration_date, status, email_verified)
VALUES (
    'admin@lmp.ca',
    '$2a$10$Jjj7emlrGWWtIyo1wLo/5.c8f/pedwHp4rWHwQcynqH4WJbYZ/OVq',
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
