-- Migration: add last_status_note to delivery_items
-- Adds a TEXT column to store the last status note for quick reads.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='delivery_items' AND column_name='last_status_note'
    ) THEN
        ALTER TABLE delivery_items ADD COLUMN last_status_note TEXT;
    END IF;
END$$;
