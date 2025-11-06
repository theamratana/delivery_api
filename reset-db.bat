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

REM Ask for confirmation
set /p CONFIRM="This will DELETE ALL DATA in the database. Continue? (y/N): "
if /i not "!CONFIRM!"=="y" (
    echo.
    echo Operation cancelled.
    goto :end
)

echo.
echo Resetting database...
psql -U %DB_USERNAME% -d deliverydb -f reset-db.sql

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo Database reset completed successfully!
    echo All tables have been cleared.
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