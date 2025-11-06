-- Quick Database Reset Script for Delivery API
-- Run this to clear all data and start fresh

-- Disable foreign key checks temporarily to avoid constraint violations
SET session_replication_role = 'replica';

-- Clear tables in correct order (respecting foreign keys)
TRUNCATE TABLE user_audits CASCADE;
TRUNCATE TABLE auth_identities CASCADE;
TRUNCATE TABLE otp_attempts CASCADE;
TRUNCATE TABLE company_invitations CASCADE;
TRUNCATE TABLE pending_employees CASCADE;
TRUNCATE TABLE employees CASCADE;
TRUNCATE TABLE user_phones CASCADE;
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE companies CASCADE;

-- Re-enable foreign key checks
SET session_replication_role = 'origin';

-- Verify all tables are empty
SELECT
    'auth_identities' as table_name,
    COUNT(*) as record_count
FROM auth_identities
UNION ALL
SELECT 'companies', COUNT(*) FROM companies
UNION ALL
SELECT 'company_invitations', COUNT(*) FROM company_invitations
UNION ALL
SELECT 'employees', COUNT(*) FROM employees
UNION ALL
SELECT 'otp_attempts', COUNT(*) FROM otp_attempts
UNION ALL
SELECT 'pending_employees', COUNT(*) FROM pending_employees
UNION ALL
SELECT 'user_audits', COUNT(*) FROM user_audits
UNION ALL
SELECT 'user_phones', COUNT(*) FROM user_phones
UNION ALL
SELECT 'users', COUNT(*) FROM users
ORDER BY table_name;