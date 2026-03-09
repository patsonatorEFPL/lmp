-- Migration pour rendre firstName et lastName optionnels
-- Permet l'inscription avec seulement email et password

-- Modifier la colonne first_name pour permettre NULL
ALTER TABLE users MODIFY COLUMN first_name VARCHAR(50) NULL;

-- Modifier la colonne last_name pour permettre NULL  
ALTER TABLE users MODIFY COLUMN last_name VARCHAR(50) NULL;

-- Commentaire pour traçabilité
-- Cette migration permet l'inscription simplifiée avec génération automatique du pseudo depuis l'email