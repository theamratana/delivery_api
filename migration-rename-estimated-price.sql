-- Migration: rename estimated_price -> last_sell_price if column present
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='products' AND column_name='estimated_price') THEN
        ALTER TABLE products RENAME COLUMN estimated_price TO last_sell_price;
    END IF;
END
$$;

-- Note: This is a safe migration for databases that may contain the older schema.
