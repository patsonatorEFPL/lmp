-- Script SQL pour ajouter les colonnes de facturation manquantes dans la table orders
-- Exécuter ce script manuellement dans MySQL

USE lmp;

-- Ajouter les colonnes de facturation manquantes (ignorer les erreurs si elles existent déjà)
ALTER TABLE orders ADD COLUMN billing_address VARCHAR(255);
ALTER TABLE orders ADD COLUMN billing_city VARCHAR(100);
ALTER TABLE orders ADD COLUMN billing_postal_code VARCHAR(20);
ALTER TABLE orders ADD COLUMN billing_country VARCHAR(100);

-- Ajouter les autres colonnes étendues
ALTER TABLE orders ADD COLUMN service_name VARCHAR(255) NOT NULL DEFAULT 'Service par défaut';
ALTER TABLE orders ADD COLUMN currency VARCHAR(3) DEFAULT 'CAD';
ALTER TABLE orders ADD COLUMN stripe_payment_intent_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN stripe_customer_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50);
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(50);
ALTER TABLE orders ADD COLUMN stripe_charge_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP NULL;
ALTER TABLE orders ADD COLUMN shipped_at TIMESTAMP NULL;
ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP NULL;
ALTER TABLE orders ADD COLUMN cancelled_at TIMESTAMP NULL;
ALTER TABLE orders ADD COLUMN tags VARCHAR(500);
ALTER TABLE orders ADD COLUMN priority INT DEFAULT 5;
ALTER TABLE orders ADD COLUMN last_modified_at TIMESTAMP NULL;
ALTER TABLE orders ADD COLUMN cancellation_reason TEXT;
ALTER TABLE orders ADD COLUMN admin_notes TEXT;
ALTER TABLE orders ADD COLUMN processing_notes TEXT;

-- Vérifier la structure de la table
DESCRIBE orders;

-- Afficher un message de confirmation
SELECT 'Colonnes de facturation ajoutées avec succès à la table orders' as message;