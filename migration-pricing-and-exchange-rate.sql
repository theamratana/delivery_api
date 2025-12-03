-- Migration: Add pricing breakdown and exchange rate tracking to delivery_items
-- Add exchange_rates table for currency conversion

-- Create exchange_rates table
CREATE TABLE IF NOT EXISTS exchange_rates (
    id UUID PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    to_currency VARCHAR(3) NOT NULL DEFAULT 'KHR',
    rate NUMERIC(10, 4) NOT NULL DEFAULT 4000.0000,
    effective_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Add indexes for exchange_rates
CREATE INDEX IF NOT EXISTS idx_exchange_rates_effective_date ON exchange_rates(effective_date);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_currencies ON exchange_rates(from_currency, to_currency);

-- Insert default USD to KHR exchange rate
INSERT INTO exchange_rates (id, from_currency, to_currency, rate, effective_date, is_active, notes)
VALUES (
    gen_random_uuid(),
    'USD',
    'KHR',
    4000.0000,
    NOW(),
    TRUE,
    'Default exchange rate: 1 USD = 4000 KHR'
) ON CONFLICT DO NOTHING;

-- Add pricing breakdown columns to delivery_items
ALTER TABLE delivery_items 
    ADD COLUMN IF NOT EXISTS delivery_discount NUMERIC(10, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS item_discount NUMERIC(10, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS order_discount NUMERIC(10, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS sub_total NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS grand_total NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS actual_delivery_cost NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS khr_amount NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS exchange_rate_used NUMERIC(10, 4);

-- Add comments to document the pricing fields
COMMENT ON COLUMN delivery_items.delivery_discount IS 'Discount applied specifically to delivery fee';
COMMENT ON COLUMN delivery_items.item_discount IS 'Discount applied to this specific item';
COMMENT ON COLUMN delivery_items.order_discount IS 'Order-wide discount applied to the total';
COMMENT ON COLUMN delivery_items.sub_total IS 'Calculated: item_value + delivery_fee - delivery_discount';
COMMENT ON COLUMN delivery_items.grand_total IS 'Calculated: sub_total - order_discount';
COMMENT ON COLUMN delivery_items.actual_delivery_cost IS 'Actual delivery cost before discount (for tracking free delivery)';
COMMENT ON COLUMN delivery_items.khr_amount IS 'Grand total converted to KHR using exchange_rate_used';
COMMENT ON COLUMN delivery_items.exchange_rate_used IS 'Exchange rate snapshot at time of transaction';
