-- Migration: Create product_categories table
-- Description: Creates the product_categories table to support multi-language product categorization
-- Date: 2025-11-13

-- Create product_categories table
CREATE TABLE IF NOT EXISTS product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    khmer_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_product_categories_code ON product_categories(code);
CREATE INDEX IF NOT EXISTS idx_product_categories_active ON product_categories(is_active);
CREATE INDEX IF NOT EXISTS idx_product_categories_sort_order ON product_categories(sort_order);

-- Insert default categories
INSERT INTO product_categories (code, name, khmer_name, sort_order) VALUES
('ELECTRONICS', 'Electronics', 'អេឡិចត្រូនិច', 1),
('CLOTHING', 'Clothing', 'សម្លៀកបំពាក់', 2),
('FOOD', 'Food', 'អាហារ', 3),
('BOOKS', 'Books', 'សៀវភៅ', 4),
('COSMETICS', 'Cosmetics', 'គ្រឿងសម្អាង', 5),
('MEDICINE', 'Medicine', 'ឱសថ', 6),
('DOCUMENTS', 'Documents', 'ឯកសារ', 7),
('OTHER', 'Other', 'ផ្សេងៗ', 99)
ON CONFLICT (code) DO NOTHING;

-- Update products table to add category_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'products'
                   AND column_name = 'category_id') THEN
        ALTER TABLE products ADD COLUMN category_id UUID;
        ALTER TABLE products ADD CONSTRAINT fk_products_category_id
            FOREIGN KEY (category_id) REFERENCES product_categories(id);
        CREATE INDEX idx_products_category_id ON products(category_id);
    END IF;
END $$;