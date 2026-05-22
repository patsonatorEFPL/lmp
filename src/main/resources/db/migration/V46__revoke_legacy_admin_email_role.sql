-- Phase out the legacy `admin@lmp.ca` account that V3 seeded as primary admin.
-- Going forward, the only admin login is the system account `Administrator`
-- (managed by DataInitializer + ADMIN_PASSWORD env). We keep the
-- `admin@lmp.ca` row to preserve order history and FK integrity, but strip
-- its ADMIN role so it can no longer authenticate as an administrator.

DELETE FROM user_roles
WHERE user_id IN (SELECT id FROM users WHERE email = 'admin@lmp.ca')
  AND role_id IN (SELECT id FROM roles WHERE name = 'ADMIN');
