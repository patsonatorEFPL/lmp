-- Script SQL pour corriger la table refunds manquante
-- Exécuter ce script manuellement dans MySQL

USE lmp;

-- Ajouter la colonne cancelled_at manquante dans la table refunds
ALTER TABLE refunds ADD COLUMN cancelled_at TIMESTAMP NULL;

-- Vérifier la structure de la table refunds
DESCRIBE refunds;

-- Afficher un message de confirmation
SELECT 'Colonne cancelled_at ajoutée avec succès à la table refunds' as message;