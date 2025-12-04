-- Migration: Add sender_name and sender_phone to delivery_items table
-- Date: 2025-12-04
-- Purpose: Denormalize sender information for historical records

ALTER TABLE delivery_items 
ADD COLUMN IF NOT EXISTS sender_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS sender_phone VARCHAR(20);

-- Verify columns were added
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'delivery_items' 
  AND column_name IN ('sender_name', 'sender_phone')
ORDER BY column_name;
