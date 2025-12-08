-- Migration: Make exchange rates company-specific
-- Description: Add company_id to exchange_rates table to support per-company exchange rates
-- Date: 2025-12-08

-- Add company_id column
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS company_id UUID;

-- Add foreign key constraint
ALTER TABLE exchange_rates ADD CONSTRAINT fk_exchange_rates_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_exchange_rates_company_id ON exchange_rates(company_id);

-- Create unique constraint: one active rate per currency pair per company
CREATE UNIQUE INDEX IF NOT EXISTS uk_exchange_rates_company_currencies_active 
    ON exchange_rates(company_id, from_currency, to_currency, is_active) 
    WHERE is_active = true AND company_id IS NOT NULL;

-- Update existing global rate to be NULL company (system default)
-- Companies without specific rates will fall back to the global rate
COMMENT ON COLUMN exchange_rates.company_id IS 'Company-specific exchange rate. NULL means system-wide default rate.';

-- Note: Existing exchange rate (company_id = NULL) will serve as the system default
-- When companies set their own rates, they will have company_id set
