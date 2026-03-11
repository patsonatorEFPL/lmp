-- V4: Add progress tracking fields to orders table
ALTER TABLE orders ADD COLUMN progress_percentage INT DEFAULT 0;
ALTER TABLE orders ADD COLUMN progress_status VARCHAR(255);
