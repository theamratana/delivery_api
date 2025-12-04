-- Clear all delivery and product related data for fresh testing
SET session_replication_role = 'replica';

-- Clear delivery-related tables
TRUNCATE TABLE delivery_photos CASCADE;
TRUNCATE TABLE delivery_packages CASCADE;
TRUNCATE TABLE delivery_items CASCADE;

-- Clear product-related tables
TRUNCATE TABLE product_photos CASCADE;
TRUNCATE TABLE products CASCADE;

-- Re-enable foreign key checks
SET session_replication_role = 'origin';

-- Verification
SELECT 'delivery_items' as table_name, COUNT(*) as record_count FROM delivery_items
UNION ALL
SELECT 'delivery_photos', COUNT(*) FROM delivery_photos
UNION ALL
SELECT 'delivery_packages', COUNT(*) FROM delivery_packages
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'product_photos', COUNT(*) FROM product_photos
ORDER BY table_name;
