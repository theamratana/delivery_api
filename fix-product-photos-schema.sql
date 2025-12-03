-- Fix product photos schema mismatch
-- Drop old product_photos and product_product_photos tables
-- Create correct product_images and images tables matching Java entities

-- Drop old tables if they exist
DROP TABLE IF EXISTS product_product_photos CASCADE;
DROP TABLE IF EXISTS product_photos CASCADE;

-- Create images table (central image storage)
CREATE TABLE IF NOT EXISTS images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    url varchar(512) NOT NULL,
    uploader_id uuid,
    company_id uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Create product_images table (join table between products and images)
CREATE TABLE IF NOT EXISTS product_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_id uuid NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    photo_index integer NOT NULL,
    UNIQUE (product_id, photo_index)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_image_id ON product_images(image_id);
CREATE INDEX IF NOT EXISTS idx_images_company_id ON images(company_id);

-- Verification
SELECT
    table_name,
    CASE
        WHEN table_name IN ('images', 'product_images') THEN 'CREATED ✓'
        ELSE 'EXISTS'
    END as status
FROM information_schema.tables
WHERE table_schema = 'public'
    AND table_name IN ('images', 'product_images', 'product_photos', 'product_product_photos')
ORDER BY table_name;
