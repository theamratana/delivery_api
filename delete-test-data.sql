-- Delete all test data for clean customer management testing

-- Delete in order respecting foreign key constraints
DELETE FROM delivery_photos;
DELETE FROM delivery_tracking;
DELETE FROM delivery_items;
DELETE FROM users WHERE user_type = 'CUSTOMER';
DELETE FROM product_photos;
DELETE FROM products;
DELETE FROM product_categories;

-- Verify deletions
SELECT 'Deliveries remaining: ' || COUNT(*) FROM delivery_items;
SELECT 'Customers remaining: ' || COUNT(*) FROM users WHERE user_type = 'CUSTOMER';
SELECT 'Products remaining: ' || COUNT(*) FROM products;
