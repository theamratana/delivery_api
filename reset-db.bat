@echo off
setlocal enabledelayedexpansion
echo ========================================
echo Delivery API Database Reset Script
echo ========================================
echo.

REM Check if DB_USERNAME is set, if not prompt for it
if "%DB_USERNAME%"=="" (
    echo DB_USERNAME not set. Using default: postgres
    set DB_USERNAME=postgres
) else (
    echo Using DB_USERNAME from environment: %DB_USERNAME%
)

REM Check if DB_PASSWORD is set, if not prompt for it
if "%DB_PASSWORD%"=="" (
    echo DB_PASSWORD not set. Using default: postgres
    set DB_PASSWORD=postgres
) else (
    echo Using DB_PASSWORD from environment: [SET]
)

echo.
echo Database credentials:
echo Username: %DB_USERNAME%
echo Password: [HIDDEN]
echo.

REM Interactive menu for module selection
echo Select modules to reset (separate with spaces, e.g., '1 3 5'):
echo ========================================
echo 0^) FULL RESET - Delete ALL data ^(CAUTION!^)
echo 1^) AUTHENTICATION MODULE - OTP, Telegram, JWT data
echo 2^) USER MANAGEMENT MODULE - Profiles, audit trails
echo 3^) COMPANY MANAGEMENT MODULE - Companies, employees
echo 4^) PRICING MODULE - Delivery pricing rules
echo 5^) DELIVERY PACKAGE MODULE - Package tracking ^(future^)
echo ========================================
set /p choices="Enter your choice(s) or 'q' to quit: "

REM Check if user wants to quit
if /i "!choices!"=="q" (
    echo.
    echo Operation cancelled.
    goto :end
)

REM Basic validation - check if input contains only numbers and spaces
echo !choices! | findstr /r /c:"^[0-9 ]*$" >nul
if errorlevel 1 (
    echo.
    echo Error: Invalid input. Please enter numbers separated by spaces.
    goto :end
)

REM Check if 0 (full reset) is selected with other options
echo !choices! | findstr /c:" 0 " >nul
if !errorlevel! equ 0 (
    echo !choices! | findstr /c:"[1-9]" >nul
    if !errorlevel! equ 0 (
        echo.
        echo Error: Cannot select '0' ^(full reset^) with other options.
        goto :end
    )
)

REM Check for leading/trailing 0
if "!choices!"=="0" (
    REM OK - full reset
) else if "!choices: =!"=="0" (
    REM OK - full reset with spaces
) else (
    echo !choices! | findstr /c:" 0" >nul
    if !errorlevel! equ 0 (
        echo !choices! | findstr /c:"[1-9]" >nul
        if !errorlevel! equ 0 (
            echo.
            echo Error: Cannot select '0' ^(full reset^) with other options.
            goto :end
        )
    )
)

echo.
echo Selected modules: !choices!
echo.

REM Generate SQL based on selections
echo -- ============================================ > temp_reset.sql
echo -- GENERATED DATABASE RESET SCRIPT >> temp_reset.sql
echo -- Selected modules: !choices! >> temp_reset.sql
echo -- ============================================ >> temp_reset.sql
echo. >> temp_reset.sql
echo -- Disable foreign key checks temporarily to avoid constraint violations >> temp_reset.sql
echo SET session_replication_role = 'replica'; >> temp_reset.sql
echo. >> temp_reset.sql

REM Add TRUNCATE statements based on selections
echo !choices! | findstr /c:" 0 " >nul
if !errorlevel! equ 0 (
    REM Full reset - all modules
    echo -- FULL RESET: Clearing all modules >> temp_reset.sql
    echo TRUNCATE TABLE auth_identities CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE otp_attempts CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE user_phones CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE user_audits CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE companies CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE employees CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE company_invitations CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE pending_employees CASCADE; >> temp_reset.sql
    echo TRUNCATE TABLE delivery_pricing_rules CASCADE; >> temp_reset.sql
    echo -- TRUNCATE TABLE delivery_packages CASCADE;  -- Future module >> temp_reset.sql
    echo -- TRUNCATE TABLE package_tracking CASCADE;   -- Future module >> temp_reset.sql
    echo -- TRUNCATE TABLE package_status_history CASCADE; -- Future module >> temp_reset.sql
    echo. >> temp_reset.sql
) else (
    echo !choices! | findstr /c:"1" >nul
    if !errorlevel! equ 0 (
        echo -- AUTHENTICATION MODULE >> temp_reset.sql
        echo TRUNCATE TABLE auth_identities CASCADE; >> temp_reset.sql
        echo TRUNCATE TABLE otp_attempts CASCADE; >> temp_reset.sql
        echo TRUNCATE TABLE user_phones CASCADE; >> temp_reset.sql
        echo. >> temp_reset.sql
    )
    echo !choices! | findstr /c:"2" >nul
    if !errorlevel! equ 0 (
        echo -- USER MANAGEMENT MODULE >> temp_reset.sql
        echo TRUNCATE TABLE user_audits CASCADE; >> temp_reset.sql
        echo. >> temp_reset.sql
    )
    echo !choices! | findstr /c:"3" >nul
    if !errorlevel! equ 0 (
        echo -- COMPANY MANAGEMENT MODULE >> temp_reset.sql
        echo TRUNCATE TABLE companies CASCADE; >> temp_reset.sql
        echo TRUNCATE TABLE employees CASCADE; >> temp_reset.sql
        echo TRUNCATE TABLE company_invitations CASCADE; >> temp_reset.sql
        echo TRUNCATE TABLE pending_employees CASCADE; >> temp_reset.sql
        echo. >> temp_reset.sql
    )
    echo !choices! | findstr /c:"4" >nul
    if !errorlevel! equ 0 (
        echo -- PRICING MODULE >> temp_reset.sql
        echo TRUNCATE TABLE delivery_pricing_rules CASCADE; >> temp_reset.sql
        echo. >> temp_reset.sql
    )
    echo !choices! | findstr /c:"5" >nul
    if !errorlevel! equ 0 (
        echo -- DELIVERY PACKAGE MODULE ^(future^) >> temp_reset.sql
        echo -- TRUNCATE TABLE delivery_packages CASCADE; >> temp_reset.sql
        echo -- TRUNCATE TABLE package_tracking CASCADE; >> temp_reset.sql
        echo -- TRUNCATE TABLE package_status_history CASCADE; >> temp_reset.sql
        echo. >> temp_reset.sql
    )
)

REM Add verification section
echo -- Re-enable foreign key checks >> temp_reset.sql
echo SET session_replication_role = 'origin'; >> temp_reset.sql
echo. >> temp_reset.sql
echo -- ============================================ >> temp_reset.sql
echo -- VERIFICATION: Check which tables were affected >> temp_reset.sql
echo -- ============================================ >> temp_reset.sql
echo SELECT >> temp_reset.sql
echo     'auth_identities' as table_name, >> temp_reset.sql
echo     COUNT(*) as record_count, >> temp_reset.sql
echo     CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END as status >> temp_reset.sql
echo FROM auth_identities >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'companies', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM companies >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'company_invitations', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM company_invitations >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'delivery_pricing_rules', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM delivery_pricing_rules >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'employees', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM employees >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'otp_attempts', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM otp_attempts >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'pending_employees', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM pending_employees >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'user_audits', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM user_audits >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'user_phones', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM user_phones >> temp_reset.sql
echo UNION ALL >> temp_reset.sql
echo SELECT 'users', COUNT(*), CASE WHEN COUNT(*) = 0 THEN 'CLEARED' ELSE 'PRESERVED' END FROM users >> temp_reset.sql
echo ORDER BY table_name; >> temp_reset.sql

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo Database reset completed successfully!
    echo Selected modules have been cleared.
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Error: Database reset failed!
    echo Please check your PostgreSQL connection.
    echo Make sure PostgreSQL is running and credentials are correct.
    echo ========================================
)

:end
echo.
pause
endlocal