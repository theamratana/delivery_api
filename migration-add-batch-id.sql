-- Add batch_id column to delivery_items and backfill
ALTER TABLE delivery_items ADD COLUMN IF NOT EXISTS batch_id uuid;
-- Backfill existing rows: set batch_id = id for legacy items so behavior stays the same
UPDATE delivery_items SET batch_id = id WHERE batch_id IS NULL;
-- Add index for faster lookup
CREATE INDEX IF NOT EXISTS idx_delivery_items_batch_id ON delivery_items(batch_id);
