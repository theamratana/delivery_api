#!/bin/bash

echo "========================================"
echo "Delivery API Database Reset Script"
echo "========================================"
echo

# Check if DB_USERNAME is set, if not use default
if [ -z "$DB_USERNAME" ]; then
    echo "DB_USERNAME not set. Using default: postgres"
    DB_USERNAME=postgres
else
    echo "Using DB_USERNAME from environment: $DB_USERNAME"
fi

# Check if DB_PASSWORD is set, if not use default
if [ -z "$DB_PASSWORD" ]; then
    echo "DB_PASSWORD not set. Using default: postgres"
    DB_PASSWORD=postgres
else
    echo "Using DB_PASSWORD from environment: [SET]"
fi

echo
echo "Database credentials:"
echo "Username: $DB_USERNAME"
echo "Password: [HIDDEN]"
echo

# Interactive menu for module selection
echo "Select modules to reset (separate with spaces, e.g., '1 3 5'):"
echo "========================================"
echo "0) FULL RESET - Delete ALL data except settings (CAUTION!)"
echo "1) AUTHENTICATION MODULE - OTP, Telegram, JWT data"
echo "2) USER MANAGEMENT MODULE - Users, profiles, audit trails"
echo "3) COMPANY MANAGEMENT MODULE - Companies, employees"
echo "4) DELIVERY MODULE - Deliveries, tracking, photos"
echo "5) PRODUCT MODULE - Products, photos"
echo "6) CUSTOMER MODULE - Customer records"
echo "7) PRICING MODULE - Delivery pricing rules"
echo "========================================"
echo "Settings PRESERVED: Provinces, Districts, Company Categories,"
echo "                    Product Categories, Exchange Rates"
echo "========================================"
echo "Enter your choice(s) or 'q' to quit:"

read -r choices

# Check if user wants to quit
if echo "$choices" | grep -q '^[Qq]$'; then
    echo
    echo "Operation cancelled."
    exit 0
fi

# Validate input
if ! echo "$choices" | grep -q '^[0-9 ]*$'; then
    echo
    echo "Error: Invalid input. Please enter numbers separated by spaces."
    exit 1
fi

# Check if 0 (full reset) is selected with other options
if echo "$choices" | grep -q '^.* 0 .*\|^0 ' || echo "$choices" | grep -q ' 0$'; then
    if echo "$choices" | grep -q '[1-9]'; then
        echo
        echo "Error: Cannot select '0' (full reset) with other options."
        exit 1
    fi
fi

echo
echo "Selected modules: $choices"
echo

# Generate SQL based on selections
SQL_CONTENT="-- ============================================
-- GENERATED DATABASE RESET SCRIPT
-- Selected modules: $choices
-- ============================================

-- Disable foreign key checks temporarily to avoid constraint violations
SET session_replication_role = 'replica';
"

# Add TRUNCATE statements based on selections
if echo "$choices" | grep -q '^0$\|^.* 0 .*\|^0 '; then
    # Full reset - all modules except settings
    SQL_CONTENT+="
-- FULL RESET: Clearing all user data (preserving settings)
-- Note: Using DELETE instead of TRUNCATE CASCADE to preserve reference tables

-- Deliveries (delete first due to foreign keys)
DELETE FROM delivery_photos;
DELETE FROM delivery_tracking;
DELETE FROM delivery_items;

-- Products (preserve product_categories - it's a setting)
DELETE FROM product_photos;
DELETE FROM products;

-- Companies (preserve company_categories - it's a setting)
DELETE FROM companies;
DELETE FROM employees;
DELETE FROM company_invitations;
DELETE FROM pending_employees;

-- Users and Customers
DELETE FROM user_audits;
DELETE FROM users WHERE user_type = 'CUSTOMER';
DELETE FROM users;  -- Delete all remaining users

-- Authentication
DELETE FROM auth_identities;
DELETE FROM otp_attempts;
DELETE FROM user_phones;

-- Pricing
DELETE FROM delivery_pricing_rules;

-- Settings PRESERVED: 
-- - provinces, districts (geographic data)
-- - company_categories (company types/industries)
-- - product_categories (product classification)
-- - exchange_rates (currency conversion rates)
"
elif echo "$choices" | grep -q '1'; then
    SQL_CONTENT+="
-- AUTHENTICATION MODULE
DELETE FROM auth_identities;
DELETE FROM otp_attempts;
DELETE FROM user_phones;
"
fi

if echo "$choices" | grep -q '2'; then
    SQL_CONTENT+="
-- USER MANAGEMENT MODULE
DELETE FROM user_audits;
DELETE FROM users;  -- Deletes all users including non-customers
"
fi

if echo "$choices" | grep -q '3'; then
    SQL_CONTENT+="
-- COMPANY MANAGEMENT MODULE
DELETE FROM companies;
DELETE FROM employees;
DELETE FROM company_invitations;
DELETE FROM pending_employees;
-- Note: company_categories preserved (it's a setting)
"
fi

if echo "$choices" | grep -q '4'; then
    SQL_CONTENT+="
-- DELIVERY MODULE
DELETE FROM delivery_photos;
DELETE FROM delivery_tracking;
DELETE FROM delivery_items;
"
fi

if echo "$choices" | grep -q '5'; then
    SQL_CONTENT+="
-- PRODUCT MODULE
DELETE FROM product_photos;
DELETE FROM products;
-- Note: product_categories preserved (it's a setting)
"
fi

if echo "$choices" | grep -q '6'; then
    SQL_CONTENT+="
-- CUSTOMER MODULE
DELETE FROM users WHERE user_type = 'CUSTOMER';
"
fi

if echo "$choices" | grep -q '7'; then
    SQL_CONTENT+="
-- PRICING MODULE
DELETE FROM delivery_pricing_rules;
"
fi

# Add verification section
SQL_CONTENT+="
-- Re-enable foreign key checks
SET session_replication_role = 'origin';

-- ============================================
-- VERIFICATION: Check which tables were affected
-- ============================================
SELECT
    'auth_identities' as table_name,
    COUNT(*) as record_count,
    CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END as status
FROM auth_identities
UNION ALL
SELECT 'companies', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM companies
UNION ALL
SELECT 'company_categories', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM company_categories
UNION ALL
SELECT 'company_invitations', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM company_invitations
UNION ALL
SELECT 'delivery_items', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM delivery_items
UNION ALL
SELECT 'delivery_photos', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM delivery_photos
UNION ALL
SELECT 'delivery_pricing_rules', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM delivery_pricing_rules
UNION ALL
SELECT 'delivery_tracking', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM delivery_tracking
UNION ALL
SELECT 'employees', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM employees
UNION ALL
SELECT 'otp_attempts', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM otp_attempts
UNION ALL
SELECT 'pending_employees', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM pending_employees
UNION ALL
SELECT 'products', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM products
UNION ALL
SELECT 'product_categories', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM product_categories
UNION ALL
SELECT 'product_photos', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM product_photos
UNION ALL
SELECT 'user_audits', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM user_audits
UNION ALL
SELECT 'user_phones', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM user_phones
UNION ALL
SELECT 'users', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM users
UNION ALL
SELECT 'customers (CUSTOMER type)', (SELECT COUNT(*) FROM users WHERE user_type = 'CUSTOMER'), 
       CASE WHEN (SELECT COUNT(*) FROM users WHERE user_type = 'CUSTOMER') = 0 THEN 'CLEARED' ELSE 'PRESERVED' END
UNION ALL
SELECT 'provinces (settings)', COUNT(*), 'PRESERVED' FROM provinces
UNION ALL
SELECT 'districts (settings)', COUNT(*), 'PRESERVED' FROM districts
UNION ALL
SELECT 'company_categories (settings)', COUNT(*), 'PRESERVED' FROM company_categories
UNION ALL
SELECT 'product_categories (settings)', COUNT(*), 'PRESERVED' FROM product_categories
UNION ALL
SELECT 'exchange_rates (settings)', COUNT(*), 'PRESERVED' FROM exchange_rates
ORDER BY table_name;
"

# Ask for confirmation
echo "This will reset the following modules:"
if echo "$choices" | grep -q '^0$\|^.* 0 .*\|^0 '; then
    echo "  - FULL RESET (ALL USER DATA - Settings Preserved)"
elif echo "$choices" | grep -q '1'; then
    echo "  - Authentication Module"
fi
if echo "$choices" | grep -q '2'; then
    echo "  - User Management Module"
fi
if echo "$choices" | grep -q '3'; then
    echo "  - Company Management Module"
fi
if echo "$choices" | grep -q '4'; then
    echo "  - Delivery Module"
fi
if echo "$choices" | grep -q '5'; then
    echo "  - Product Module"
fi
if echo "$choices" | grep -q '6'; then
    echo "  - Customer Module"
fi
if echo "$choices" | grep -q '7'; then
    echo "  - Pricing Module"
fi

echo
read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo
    echo "Operation cancelled."
    exit 0
fi

echo
echo "Resetting database..."

# Execute the generated SQL
echo "$SQL_CONTENT" | docker exec -i delivery-postgres psql -U $DB_USERNAME -d deliverydb

if [ $? -eq 0 ]; then
    echo
    echo "========================================"
    echo "Database reset completed successfully!"
    echo "Selected modules have been cleared."
    echo "========================================"
else
    echo
    echo "========================================"
    echo "Error: Database reset failed!"
    echo "Please check your PostgreSQL connection."
    echo "Make sure PostgreSQL is running and credentials are correct."
    echo "========================================"
fi