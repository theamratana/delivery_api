-- Migration: Add payment_method column to delivery_items table
-- Date: 2025-11-14
-- Description: Add payment method tracking (COD or PAID) for deliveries

ALTER TABLE delivery_items ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) NOT NULL DEFAULT 'COD';

-- Update existing null values to COD (if any)
UPDATE delivery_items SET payment_method = 'COD' WHERE payment_method IS NULL;

-- Optional: Add index on payment_method for faster queries
CREATE INDEX IF NOT EXISTS idx_delivery_items_payment_method ON delivery_items(payment_method);

-- Verify the migration
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'delivery_items' AND column_name = 'payment_method';
