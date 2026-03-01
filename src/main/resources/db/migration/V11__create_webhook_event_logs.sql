-- V11: Table d'idempotence pour les webhooks
-- Empêche le traitement en double des événements Stripe

CREATE TABLE IF NOT EXISTS webhook_event_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100),
    provider VARCHAR(50) NOT NULL,
    processed_at DATETIME NOT NULL,
    status VARCHAR(50),
    processing_error TEXT,
    INDEX idx_webhook_event_id (event_id),
    INDEX idx_webhook_provider (provider),
    INDEX idx_webhook_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
