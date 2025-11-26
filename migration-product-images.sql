-- Migration to create product_images table and ensure images table exists
CREATE TABLE IF NOT EXISTS images (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  url varchar(512) not null,
  uploader_id uuid,
  company_id uuid,
  created_at timestamptz not null DEFAULT now()
);

CREATE TABLE IF NOT EXISTS product_images (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  image_id uuid NOT NULL REFERENCES images(id) ON DELETE CASCADE,
  photo_index integer NOT NULL
);
