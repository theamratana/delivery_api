-- Add company tracking and default address fields to users table for customer management

-- Add company_id for company-scoped customers
ALTER TABLE users ADD COLUMN company_id UUID;
ALTER TABLE users ADD CONSTRAINT fk_users_company 
  FOREIGN KEY (company_id) REFERENCES companies(id);

-- Add default address fields for customers
ALTER TABLE users ADD COLUMN default_address TEXT;
ALTER TABLE users ADD COLUMN default_province_id UUID;
ALTER TABLE users ADD COLUMN default_district_id UUID;

ALTER TABLE users ADD CONSTRAINT fk_users_default_province 
  FOREIGN KEY (default_province_id) REFERENCES provinces(id);
ALTER TABLE users ADD CONSTRAINT fk_users_default_district 
  FOREIGN KEY (default_district_id) REFERENCES districts(id);

-- Create unique index: one phone per company (for CUSTOMER type only)
CREATE UNIQUE INDEX uk_users_phone_company 
  ON users(phone_e164, company_id) 
  WHERE user_type = 'CUSTOMER' AND company_id IS NOT NULL;

-- Create indexes for better query performance
CREATE INDEX idx_users_company_id ON users(company_id) WHERE user_type = 'CUSTOMER';
CREATE INDEX idx_users_phone_company ON users(phone_e164, company_id) WHERE user_type = 'CUSTOMER';

COMMENT ON COLUMN users.company_id IS 'For CUSTOMER type: which company owns this customer record. NULL for other user types.';
COMMENT ON COLUMN users.default_address IS 'Default delivery address for this customer';
COMMENT ON COLUMN users.default_province_id IS 'Default province for customer deliveries';
COMMENT ON COLUMN users.default_district_id IS 'Default district for customer deliveries';
