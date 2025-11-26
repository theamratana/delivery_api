-- Baseline migration: Current stable schema snapshot (partial)
-- This file creates the core tables and columns used by the application
-- Note: This is intended for new DB setups only. Existing databases should migrate in place.

-- Companies
CREATE TABLE IF NOT EXISTS companies (
    id uuid PRIMARY KEY,
    name varchar(255),
    created_at timestamptz,
    updated_at timestamptz
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY,
    display_name varchar(255),
    username varchar(50) UNIQUE,
    first_name varchar(100),
    last_name varchar(100),
    avatar_url varchar(512),
    phone_e164 varchar(50),
    phone_verified_at timestamptz,
    email varchar(255),
    email_verified_at timestamptz,
    user_type varchar(50),
    company_id uuid REFERENCES companies(id),
    user_role varchar(50),
    is_incomplete boolean default false,
    active boolean default true,
    last_login_at timestamptz,
    created_at timestamptz,
    updated_at timestamptz
);

-- Product categories
CREATE TABLE IF NOT EXISTS product_categories (
    id uuid PRIMARY KEY,
    code varchar(50) UNIQUE,
    name varchar(255),
    created_at timestamptz,
    updated_at timestamptz
);

-- Products
CREATE TABLE IF NOT EXISTS products (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    description text,
    category_id uuid REFERENCES product_categories(id),
    default_price numeric(10,2) DEFAULT 0,
    buying_price numeric(10,2) DEFAULT 0,
    selling_price numeric(10,2) DEFAULT 0,
    last_sell_price numeric(10,2) DEFAULT 0,
    weight_kg numeric(5,2),
    dimensions varchar(50),
    is_active boolean NOT NULL DEFAULT true,
    is_published boolean NOT NULL DEFAULT false,
    usage_count integer NOT NULL DEFAULT 0,
    last_used_at timestamptz,
    created_at timestamptz,
    updated_at timestamptz
);

-- Images
CREATE TABLE IF NOT EXISTS images (
    id uuid PRIMARY KEY,
    url varchar(512) NOT NULL,
    uploader_id uuid REFERENCES users(id),
    company_id uuid REFERENCES companies(id),
    created_at timestamptz
);

-- Product images mapping (ordered)
CREATE TABLE IF NOT EXISTS product_images (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_id uuid NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    photo_index int NOT NULL
);

-- Existing column rename helper
-- If your database has a column named estimated_price on products, run the following manually
-- ALTER TABLE products RENAME COLUMN estimated_price TO last_sell_price;

-- End of baseline migration
