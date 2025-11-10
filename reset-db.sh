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
echo "0) FULL RESET - Delete ALL data (CAUTION!)"
echo "1) AUTHENTICATION MODULE - OTP, Telegram, JWT data"
echo "2) USER MANAGEMENT MODULE - Profiles, audit trails"
echo "3) COMPANY MANAGEMENT MODULE - Companies, employees"
echo "4) PRICING MODULE - Delivery pricing rules"
echo "5) DELIVERY PACKAGE MODULE - Package tracking (future)"
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
    # Full reset - all modules
    SQL_CONTENT+="
-- FULL RESET: Clearing all modules
TRUNCATE TABLE auth_identities CASCADE;
TRUNCATE TABLE otp_attempts CASCADE;
TRUNCATE TABLE user_phones CASCADE;
TRUNCATE TABLE user_audits CASCADE;
TRUNCATE TABLE companies CASCADE;
TRUNCATE TABLE employees CASCADE;
TRUNCATE TABLE company_invitations CASCADE;
TRUNCATE TABLE pending_employees CASCADE;
TRUNCATE TABLE delivery_pricing_rules CASCADE;
-- TRUNCATE TABLE delivery_packages CASCADE;  -- Future module
-- TRUNCATE TABLE package_tracking CASCADE;   -- Future module
-- TRUNCATE TABLE package_status_history CASCADE; -- Future module
"
elif echo "$choices" | grep -q '1'; then
    SQL_CONTENT+="
-- AUTHENTICATION MODULE
TRUNCATE TABLE auth_identities CASCADE;
TRUNCATE TABLE otp_attempts CASCADE;
TRUNCATE TABLE user_phones CASCADE;
"
fi

if echo "$choices" | grep -q '2'; then
    SQL_CONTENT+="
-- USER MANAGEMENT MODULE
TRUNCATE TABLE user_audits CASCADE;
"
fi

if echo "$choices" | grep -q '3'; then
    SQL_CONTENT+="
-- COMPANY MANAGEMENT MODULE
TRUNCATE TABLE companies CASCADE;
TRUNCATE TABLE employees CASCADE;
TRUNCATE TABLE company_invitations CASCADE;
TRUNCATE TABLE pending_employees CASCADE;
"
fi

if echo "$choices" | grep -q '4'; then
    SQL_CONTENT+="
-- PRICING MODULE
TRUNCATE TABLE delivery_pricing_rules CASCADE;
"
fi

if echo "$choices" | grep -q '5'; then
    SQL_CONTENT+="
-- DELIVERY PACKAGE MODULE (future)
-- TRUNCATE TABLE delivery_packages CASCADE;
-- TRUNCATE TABLE package_tracking CASCADE;
-- TRUNCATE TABLE package_status_history CASCADE;
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
SELECT 'company_invitations', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM company_invitations
UNION ALL
SELECT 'delivery_pricing_rules', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM delivery_pricing_rules
UNION ALL
SELECT 'employees', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM employees
UNION ALL
SELECT 'otp_attempts', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM otp_attempts
UNION ALL
SELECT 'pending_employees', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM pending_employees
UNION ALL
SELECT 'user_audits', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM user_audits
UNION ALL
SELECT 'user_phones', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM user_phones
UNION ALL
SELECT 'users', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM users
ORDER BY table_name;
"

# Ask for confirmation
echo "This will reset the following modules:"
if echo "$choices" | grep -q '^0$\|^.* 0 .*\|^0 '; then
    echo "  - FULL RESET (ALL DATA)"
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
    echo "  - Pricing Module"
fi
if echo "$choices" | grep -q '5'; then
    echo "  - Delivery Package Module (future)"
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