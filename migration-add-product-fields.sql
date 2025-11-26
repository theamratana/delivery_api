-- Migration: Add buyingPrice, sellingPrice, isPublished columns
-- Adds columns to store product's buying_price, selling_price and publish status.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS buying_price numeric(10,2) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS selling_price numeric(10,2) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_published boolean NOT NULL DEFAULT false;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS last_sell_price numeric(10,2) NOT NULL DEFAULT 0;

-- Create product_photos table for product photos
CREATE TABLE IF NOT EXISTS product_photos (
    product_id uuid NOT NULL,
    photo_index int NOT NULL,
    photo_url varchar(512) NOT NULL,
    PRIMARY KEY (product_id, photo_index),
    CONSTRAINT fk_pp_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
