-- Script pour corriger les contraintes de base de données pour le hard delete
-- À exécuter manuellement dans MySQL

USE lmp;

-- 1. Vérifier l'état actuel des contraintes
SELECT 
    TABLE_NAME, 
    COLUMN_NAME, 
    IS_NULLABLE, 
    CONSTRAINT_NAME
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'lmp' 
    AND TABLE_NAME IN ('orders', 'reviews') 
    AND COLUMN_NAME = 'user_id';

-- 2. Modifier orders.user_id pour permettre NULL
ALTER TABLE orders MODIFY COLUMN user_id BIGINT NULL;

-- 3. Modifier reviews.user_id pour permettre NULL
ALTER TABLE reviews MODIFY COLUMN user_id BIGINT NULL;

-- 4. Vérifier que les changements ont été appliqués
SELECT 
    TABLE_NAME, 
    COLUMN_NAME, 
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'lmp' 
    AND TABLE_NAME IN ('orders', 'reviews') 
    AND COLUMN_NAME = 'user_id';

-- 5. Tester l'anonymisation sur une commande de test (si nécessaire)
-- UPDATE orders SET user_id = NULL WHERE id = 1;
-- SELECT * FROM orders WHERE id = 1;
