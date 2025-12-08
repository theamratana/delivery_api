-- Migration: Create company_categories table with seed data
-- Created: 2025-12-05

CREATE TABLE IF NOT EXISTS company_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    name_km VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index on code for faster lookups
CREATE INDEX IF NOT EXISTS idx_company_categories_code ON company_categories(code);

-- Seed default categories
INSERT INTO company_categories (code, name, name_km) VALUES
    ('DELIVERY', 'Delivery', 'សេវាដឹកជញ្ជូន'),
    ('JEWELRY', 'Jewelry Store', 'ហាងគ្រឿងអលង្ការ')
ON CONFLICT (code) DO NOTHING;
