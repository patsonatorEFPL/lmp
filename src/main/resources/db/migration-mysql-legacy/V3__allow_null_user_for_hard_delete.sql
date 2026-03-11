-- Migration pour permettre l'anonymisation des données lors du hard delete
-- Permet de mettre user_id à NULL dans orders et reviews

-- Vérifier et modifier la contrainte sur orders.user_id pour permettre NULL
SET @query = (
    SELECT CONCAT('ALTER TABLE orders MODIFY COLUMN user_id BIGINT NULL')
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'orders' 
    AND COLUMN_NAME = 'user_id' 
    AND IS_NULLABLE = 'NO'
    AND TABLE_SCHEMA = DATABASE()
);

SET @query = IFNULL(@query, 'SELECT "orders.user_id already allows NULL" as message');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Vérifier et modifier la contrainte sur reviews.user_id pour permettre NULL  
SET @query = (
    SELECT CONCAT('ALTER TABLE reviews MODIFY COLUMN user_id BIGINT NULL')
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'reviews' 
    AND COLUMN_NAME = 'user_id' 
    AND IS_NULLABLE = 'NO'
    AND TABLE_SCHEMA = DATABASE()
);

SET @query = IFNULL(@query, 'SELECT "reviews.user_id already allows NULL" as message');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Note: Les foreign keys restent en place pour maintenir l'intégrité quand user_id est présent
-- Mais permettent maintenant user_id = NULL pour l'anonymisation
