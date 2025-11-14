-- Migration Script: Add delivery_pricing_rules table
-- Run this after the initial schema is created

-- Create delivery_pricing_rules table
CREATE TABLE IF NOT EXISTS delivery_pricing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    rule_name VARCHAR(255) NOT NULL,
    province VARCHAR(255),
    district VARCHAR(255),
    base_fee DECIMAL(10,2) NOT NULL CHECK (base_fee >= 0),
    high_value_surcharge DECIMAL(10,2) DEFAULT 0 CHECK (high_value_surcharge >= 0),
    high_value_threshold DECIMAL(10,2) DEFAULT 0 CHECK (high_value_threshold >= 0),
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_delivery_pricing_rules_company_id ON delivery_pricing_rules(company_id);
CREATE INDEX IF NOT EXISTS idx_delivery_pricing_rules_province ON delivery_pricing_rules(province);
CREATE INDEX IF NOT EXISTS idx_delivery_pricing_rules_district ON delivery_pricing_rules(district);
CREATE INDEX IF NOT EXISTS idx_delivery_pricing_rules_priority ON delivery_pricing_rules(priority DESC);
CREATE INDEX IF NOT EXISTS idx_delivery_pricing_rules_active ON delivery_pricing_rules(is_active) WHERE is_active = true;

-- Create a partial index for active rules by company and location
CREATE INDEX IF NOT EXISTS idx_delivery_pricing_rules_company_location
ON delivery_pricing_rules(company_id, province, district, priority DESC)
WHERE is_active = true;

-- Add comments for documentation
COMMENT ON TABLE delivery_pricing_rules IS 'Stores company-defined pricing rules for delivery fees based on geographic locations';
COMMENT ON COLUMN delivery_pricing_rules.rule_name IS 'Human-readable name for the pricing rule';
COMMENT ON COLUMN delivery_pricing_rules.province IS 'Province for which this rule applies (null for global rules)';
COMMENT ON COLUMN delivery_pricing_rules.district IS 'District for which this rule applies (null for province-wide rules)';
COMMENT ON COLUMN delivery_pricing_rules.base_fee IS 'Base delivery fee for this rule';
COMMENT ON COLUMN delivery_pricing_rules.high_value_surcharge IS 'Additional fee for high-value items';
COMMENT ON COLUMN delivery_pricing_rules.high_value_threshold IS 'Value threshold above which high_value_surcharge applies';
COMMENT ON COLUMN delivery_pricing_rules.priority IS 'Rule priority (higher values take precedence)';
COMMENT ON COLUMN delivery_pricing_rules.is_active IS 'Whether this rule is currently active';

-- Insert some default pricing rules for demonstration
-- Note: Replace 'default-company-id' with actual company IDs when running this script
-- These are example rules that can be customized per company

-- Example: Global default rule (lowest priority)
-- INSERT INTO delivery_pricing_rules (company_id, rule_name, base_fee, priority)
-- VALUES ('00000000-0000-0000-0000-000000000000', 'Global Default', 2.00, 0);

-- Example: Phnom Penh specific rule
-- INSERT INTO delivery_pricing_rules (company_id, rule_name, province, base_fee, priority)
-- VALUES ('00000000-0000-0000-0000-000000000000', 'Phnom Penh Delivery', 'Phnom Penh', 2.00, 10);

-- Example: Province-wide rule for other provinces
-- INSERT INTO delivery_pricing_rules (company_id, rule_name, province, base_fee, priority)
-- VALUES ('00000000-0000-0000-0000-000000000000', 'Province Delivery', NULL, 1.50, 5);

-- Verify the table was created successfully
SELECT
    'delivery_pricing_rules' as table_name,
    COUNT(*) as record_count
FROM delivery_pricing_rules;