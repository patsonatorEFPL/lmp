-- Migration V8: Mise à jour de la table order_status_history pour le module d'administration
-- Ajoute les colonnes nécessaires pour l'intégration avec les nouvelles entités admin

-- Ajouter les nouvelles colonnes pour les statuts étendus et l'audit complet
ALTER TABLE order_status_history 
ADD COLUMN from_status VARCHAR(50) AFTER order_id,
ADD COLUMN to_status VARCHAR(50) NOT NULL AFTER from_status,
ADD COLUMN changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER to_status,
ADD COLUMN changed_by VARCHAR(100) AFTER changed_at,
ADD COLUMN created_by_user_id BIGINT AFTER changed_by,
ADD COLUMN note TEXT AFTER created_by_user_id;

-- Mettre à jour les données existantes pour la compatibilité
UPDATE order_status_history 
SET to_status = status,
    changed_at = created_at,
    changed_by = (SELECT email FROM users WHERE id = order_status_history.created_by),
    created_by_user_id = created_by,
    note = notes
WHERE to_status IS NULL;

-- Ajouter la contrainte de clé étrangère pour created_by_user_id
ALTER TABLE order_status_history 
ADD CONSTRAINT fk_order_status_history_created_by_user 
FOREIGN KEY (created_by_user_id) REFERENCES users(id);

-- Mettre à jour les types ENUM pour supporter les nouveaux statuts (incluant PAYMENT_PENDING existant)
ALTER TABLE order_status_history
MODIFY COLUMN to_status ENUM(
    'PENDING',
    'PAYMENT_PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
    'REFUNDED',
    'IN_PROGRESS',
    'COMPLETED',
    'UNDER_REVIEW'
) NOT NULL;

ALTER TABLE order_status_history
MODIFY COLUMN from_status ENUM(
    'PENDING',
    'PAYMENT_PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
    'REFUNDED',
    'IN_PROGRESS',
    'COMPLETED',
    'UNDER_REVIEW'
);

-- Mettre à jour également la table orders pour supporter les nouveaux statuts (incluant PAYMENT_PENDING existant)
ALTER TABLE orders
MODIFY COLUMN status ENUM(
    'PENDING',
    'PAYMENT_PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
    'REFUNDED',
    'IN_PROGRESS',
    'COMPLETED',
    'UNDER_REVIEW'
) DEFAULT 'PENDING';

-- Créer la table refunds pour l'administration des remboursements
CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CAD',
    reason VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    stripe_refund_id VARCHAR(100),
    is_partial_refund BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at DATETIME,
    failure_reason TEXT,
    processed_by BIGINT,
    cancellation_reason TEXT,
    created_by_admin_id BIGINT,
    
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (processed_by) REFERENCES users(id),
    FOREIGN KEY (created_by_admin_id) REFERENCES users(id),
    
    INDEX idx_refunds_order_id (order_id),
    INDEX idx_refunds_status (status),
    INDEX idx_refunds_stripe_id (stripe_refund_id),
    INDEX idx_refunds_created_at (created_at)
);

-- Ajouter des champs manquants à la table orders pour l'administration
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS last_modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
ADD COLUMN IF NOT EXISTS admin_notes TEXT,
ADD COLUMN IF NOT EXISTS processing_notes TEXT;

-- Ajouter des index pour améliorer les performances des requêtes admin
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id ON order_status_history(order_id);
CREATE INDEX IF NOT EXISTS idx_order_status_history_changed_at ON order_status_history(changed_at);
CREATE INDEX IF NOT EXISTS idx_order_status_history_created_by_user ON order_status_history(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_last_modified ON orders(last_modified_at);