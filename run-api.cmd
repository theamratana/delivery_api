@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Defaults for local Docker Postgres mapping (host 5433 -> container 5432)
if "%DB_URL%"=="" set DB_URL=jdbc:postgresql://localhost:5433/deliverydb
if "%DB_USERNAME%"=="" set DB_USERNAME=postgres
if "%DB_PASSWORD%"=="" set DB_PASSWORD=postgres
if "%SERVER_PORT%"=="" set SERVER_PORT=8081

set "CMD=%~1"
if "%CMD%"=="" set "CMD=start"

if "%CMD%"=="help" goto :help
if "%CMD%"=="--help" goto :help
if "%CMD%"=="-h" goto :help
if "%CMD%"=="stop" goto :stop
if "%CMD%"=="status" goto :status
if "%CMD%"=="restart" goto :restart
if "%CMD%"=="start" goto :start
goto :start

:help
echo Usage: run-api.cmd [command]
echo.
echo Commands:
echo   start         Start API locally with Gradle (PostgreSQL in Docker)
echo   stop          Stop API (keeps PostgreSQL running)
echo   restart       Stop then start API
echo   status        Check if API is running
echo   help          Show this help message
echo.
echo Examples:
echo   run-api.cmd start          # Start API locally
echo   run-api.cmd restart        # Restart API after code changes
echo   run-api.cmd stop           # Stop API (PostgreSQL stays running)
echo.
echo Notes:
echo   - API runs locally via Gradle bootRun
echo   - PostgreSQL runs in Docker on port 5433
echo   - To stop PostgreSQL: docker compose down
echo.
echo Access API at: http://localhost:%SERVER_PORT%/api/
goto :eof

:stop
echo Stopping API server on port %SERVER_PORT%...
rem Stop local API process (keep PostgreSQL running in Docker)
rem If you want to stop PostgreSQL too, use: docker compose down
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%SERVER_PORT%') do (
    taskkill /F /PID %%a >nul 2>&1
)
if exist .pid_api del .pid_api
echo ✅ API server stopped successfully.
goto :eof

:status
netstat -ano | findstr :%SERVER_PORT% >nul 2>&1
if %ERRORLEVEL%==0 (
    echo ✅ Port %SERVER_PORT% is IN USE (API server running)
    exit /b 0
) else (
    echo ℹ️  Port %SERVER_PORT% is FREE (no API server running)
    exit /b 1
)

:restart
echo Restarting API server...
call :stop
timeout /t 2 /nobreak >nul
gstart
rem Start PostgreSQL in Docker
echo 🐳 Starting PostgreSQL in Docker...
docker compose up -d postgres >nul 2>&1
if %ERRORLEVEL%==0 (
    timeout /t 3 /nobreak >nul
    echo ✅ PostgreSQL container started
) else (
    echo ⚠️  Docker not available. Ensure PostgreSQL is running on port 5433
)
eof

:start
rem Check if port is in use and stop existing process
netstat -ano | findstr :%SERVER_PORT% >nul 2>&1
if %ERRORLEVEL%==0 (
    echo Port %SERVER_PORT% is busy. Stopping existing process...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%SERVER_PORT%') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 1 /nobreak >nul
    netstat -ano | findstr :%SERVER_PORT% >nul 2>&1
    if %ERRORLEVEL%==0 (
        echo ❌ ERROR: Could not free port %SERVER_PORT%.
        exit /b 1
    )
)

echo 🚀 Starting API server on port %SERVER_PORT%...
if exist gradlew.bat (
    start /B gradlew.bat bootRun -Dspring.profiles.active=production
) else (
    start /B gradle-8.5\bin\gradle.bat bootRun -Dspring.profiles.active=production
)
echo ✅ API server started.
goto :eof
echo    Database: %DB_URL%
if exist gradlew.bat (
    start /B gradlew.bat bootRun
) else (
    start /B gradle-8.5\bin\gradle.bat bootRun
)
echo ✅ API server started.
echo    Access at: http://localhost:%SERVER_PORT%/api/