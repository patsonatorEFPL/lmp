-- Correction des mots de passe avec les bons hashes BCrypt
-- admin123 : $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.
-- user123  : $2a$10$N.ew/cBKt3.GYd5dQqGVwOm3cA6U3g86Xo7y9B1zQw7j8F2a7L8Qa

UPDATE users 
SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' 
WHERE email = 'admin@lmp.ca';

UPDATE users 
SET password = '$2a$10$N.ew/cBKt3.GYd5dQqGVwOm3cA6U3g86Xo7y9B1zQw7j8F2a7L8Qa' 
WHERE email = 'user@lmp.ca';