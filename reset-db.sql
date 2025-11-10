-- ============================================
-- MODULAR DATABASE RESET SCRIPT for Delivery API
-- ============================================
-- This script allows selective reset of specific modules/features
-- Uncomment the sections you want to reset, comment out what you want to preserve

-- INSTRUCTIONS:
-- 1. Uncomment ONLY the module(s) you're working on and having issues with
-- 2. Run the script: ./reset-db.sh (Linux/Mac) or reset-db.bat (Windows)
-- 3. Check the verification output to confirm only intended tables were cleared

-- Disable foreign key checks temporarily to avoid constraint violations
SET session_replication_role = 'replica';

-- ============================================
-- CURRENT CONFIGURATION: PRICING MODULE RESET
-- ============================================
-- Only delivery_pricing_rules will be cleared
-- All other modules (users, companies, auth, etc.) will be preserved
-- To change this, uncomment the desired modules below

-- AUTHENTICATION MODULE (OTP, Telegram, JWT)
-- Uncomment to reset all authentication-related data
-- TRUNCATE TABLE auth_identities CASCADE;
-- TRUNCATE TABLE otp_attempts CASCADE;
-- TRUNCATE TABLE user_phones CASCADE;

-- USER MANAGEMENT MODULE (Profiles, Audit)
-- Uncomment to reset user profiles and audit trails
-- TRUNCATE TABLE user_audits CASCADE;

-- COMPANY MANAGEMENT MODULE (Companies, Employees)
-- Uncomment to reset company and employee data
-- TRUNCATE TABLE companies CASCADE;
-- TRUNCATE TABLE employees CASCADE;
-- TRUNCATE TABLE company_invitations CASCADE;
-- TRUNCATE TABLE pending_employees CASCADE;

-- PRICING MODULE (Delivery Pricing Rules)
-- Uncomment to reset pricing configuration
TRUNCATE TABLE delivery_pricing_rules CASCADE;

-- DELIVERY PACKAGE MODULE (when implemented)
-- Uncomment when working on DeliveryPackage feature
-- TRUNCATE TABLE delivery_packages CASCADE;
-- TRUNCATE TABLE package_tracking CASCADE;
-- TRUNCATE TABLE package_status_history CASCADE;

-- FULL RESET (CAUTION: Clears ALL data)
-- Uncomment ALL sections above for complete data wipe
-- WARNING: This will delete all working data including users, companies, etc.

-- ============================================
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