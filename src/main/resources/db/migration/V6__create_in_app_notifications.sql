-- =============================================
-- V6: In-app notifications persistence
-- =============================================

CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    order_id VARCHAR(100),
    service_name VARCHAR(255),
    amount DECIMAL(10, 2),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_in_app_notif_user_id ON in_app_notifications(user_id);
CREATE INDEX idx_in_app_notif_user_read ON in_app_notifications(user_id, is_read);
CREATE INDEX idx_in_app_notif_created_at ON in_app_notifications(created_at DESC);
