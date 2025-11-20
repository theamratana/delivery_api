-- Migration: Add buyingPrice, sellingPrice, isPublished columns
-- Adds columns to store product's buying_price, selling_price and publish status.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS buying_price numeric(10,2) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS selling_price numeric(10,2) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_published boolean NOT NULL DEFAULT false;
