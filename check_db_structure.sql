-- Vérification de la structure des tables pour hard delete
-- Exécuter ce script pour vérifier si les migrations ont été appliquées

-- 1. Vérifier la structure de la table orders
DESCRIBE orders;

-- 2. Vérifier la structure de la table reviews
DESCRIBE reviews;

-- 3. Vérifier les contraintes sur user_id
SELECT 
    COLUMN_NAME, 
    IS_NULLABLE, 
    DATA_TYPE, 
    COLUMN_KEY, 
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME IN ('orders', 'reviews') 
    AND COLUMN_NAME = 'user_id' 
    AND TABLE_SCHEMA = DATABASE();

-- 4. Vérifier les migrations appliquées
SELECT * FROM flyway_schema_history ORDER BY installed_on;

-- 5. Vérifier s'il y a des utilisateurs avec des données liées
SELECT 
    u.id,
    u.email,
    COUNT(DISTINCT o.id) as orders_count,
    COUNT(DISTINCT r.id) as reviews_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
LEFT JOIN reviews r ON u.id = r.user_id
GROUP BY u.id, u.email
HAVING orders_count > 0 OR reviews_count > 0;
