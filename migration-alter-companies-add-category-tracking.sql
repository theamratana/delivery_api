-- Migration: Alter companies table - add category, phone, province, tracking fields
-- Created: 2025-12-05

-- Add category_id column (FK to company_categories)
ALTER TABLE companies ADD COLUMN IF NOT EXISTS category_id UUID;

-- Add phone_number column
ALTER TABLE companies ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

-- Add province_id column (FK to provinces) - allows null district but require province
ALTER TABLE companies ADD COLUMN IF NOT EXISTS province_id UUID;

-- Make district_id nullable (optional)
ALTER TABLE companies ALTER COLUMN district_id DROP NOT NULL;

-- Add tracking columns for who created/updated
ALTER TABLE companies ADD COLUMN IF NOT EXISTS created_by_user_id UUID;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS updated_by_user_id UUID;

-- Add column to track which company's delivery auto-created this company
ALTER TABLE companies ADD COLUMN IF NOT EXISTS created_by_company_id UUID;

-- Drop old unique constraint on name only
ALTER TABLE companies DROP CONSTRAINT IF EXISTS uk_companies_name;

-- Add new unique constraint on name + created_by_company_id
ALTER TABLE companies ADD CONSTRAINT uk_companies_name_created_by 
    UNIQUE (name, created_by_company_id);

-- Add foreign key constraints
ALTER TABLE companies ADD CONSTRAINT fk_companies_category 
    FOREIGN KEY (category_id) REFERENCES company_categories(id);

ALTER TABLE companies ADD CONSTRAINT fk_companies_province 
    FOREIGN KEY (province_id) REFERENCES provinces(id);

ALTER TABLE companies ADD CONSTRAINT fk_companies_created_by_user 
    FOREIGN KEY (created_by_user_id) REFERENCES users(id);

ALTER TABLE companies ADD CONSTRAINT fk_companies_updated_by_user 
    FOREIGN KEY (updated_by_user_id) REFERENCES users(id);

ALTER TABLE companies ADD CONSTRAINT fk_companies_created_by_company 
    FOREIGN KEY (created_by_company_id) REFERENCES companies(id);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_companies_category_id ON companies(category_id);
CREATE INDEX IF NOT EXISTS idx_companies_province_id ON companies(province_id);
CREATE INDEX IF NOT EXISTS idx_companies_created_by_user_id ON companies(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_companies_created_by_company_id ON companies(created_by_company_id);
CREATE INDEX IF NOT EXISTS idx_companies_name_created_by ON companies(name, created_by_company_id);

-- Set default category to 'DELIVERY' for existing companies
UPDATE companies 
SET category_id = (SELECT id FROM company_categories WHERE code = 'DELIVERY' LIMIT 1)
WHERE category_id IS NULL;

-- Set province_id from district_id for existing companies
UPDATE companies c
SET province_id = (SELECT d.province_id FROM districts d WHERE d.id = c.district_id)
WHERE c.province_id IS NULL AND c.district_id IS NOT NULL;
