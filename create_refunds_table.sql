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
ADD COLUMN last_modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
ADD COLUMN cancellation_reason TEXT,
ADD COLUMN admin_notes TEXT,
ADD COLUMN processing_notes TEXT;

-- Marquer la migration V8 comme réussie
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) 
VALUES (7, '8', 'update order status history for admin', 'SQL', 'V8__update_order_status_history_for_admin.sql', 1352508966, 'root', NOW(), 100, 1);