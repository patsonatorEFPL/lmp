-- V7: Add stripe_session_id column to orders table
-- This migration adds the stripe session ID to link orders with Stripe checkout sessions

ALTER TABLE orders 
ADD COLUMN stripe_session_id VARCHAR(255) DEFAULT NULL COMMENT 'Stripe checkout session ID for linking orders to Stripe sessions';

-- Add index for performance when looking up orders by stripe session ID
CREATE INDEX idx_orders_stripe_session_id ON orders(stripe_session_id);