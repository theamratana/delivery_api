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
if "%CMD%"=="rebuild" goto :rebuild
if "%CMD%"=="start-clean" goto :start_clean
if "%CMD%"=="clean" goto :start_clean
if "%CMD%"=="start" goto :start
goto :start

:help
echo Usage: run-api.cmd [command]
echo.
echo Commands:
echo   start         Start API (uses existing Docker image if available)
echo   stop          Stop API and containers
echo   restart       Stop then start API
echo   status        Check if API is running
echo   rebuild       Quick rebuild with latest code (uses cache, faster) 🔄
echo   start-clean   Full clean rebuild (no cache, guaranteed fresh) 🧹
echo   clean         Alias for start-clean
echo   help          Show this help message
echo.
echo Examples:
echo   run-api.cmd start          # Normal start
echo   run-api.cmd rebuild        # After code changes (recommended)
echo   run-api.cmd start-clean    # When rebuild doesn't work
echo   run-api.cmd stop           # Stop everything
echo.
echo Access API at: http://localhost:%SERVER_PORT%/api/
goto :eof

:stop
echo Stopping API server on port %SERVER_PORT%...
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
goto :start

:rebuild
echo 🔄 Rebuilding with latest code...
docker compose stop api 2>nul
echo    Step 1: Building Gradle (incremental)...
call gradlew.bat build -x test
if %ERRORLEVEL% neq 0 (
    echo ❌ Gradle build failed
    exit /b 1
)
echo    Step 2: Rebuilding Docker image...
docker compose build api
echo    Step 3: Starting containers...
docker compose up -d postgres api
echo ✅ Docker containers started with latest code
echo    Waiting for API to be ready...
timeout /t 10 /nobreak >nul
curl -s http://localhost:%SERVER_PORT%/api/auth/dev/token/00000000-0000-0000-0000-000000000000 >nul 2>&1
if %ERRORLEVEL%==0 (
    echo ✅ API is ready!
) else (
    echo ⚠️  API started but may still be initializing. Check logs: docker compose logs api
)
goto :eof

:start_clean
echo 🧹 Cleaning and rebuilding Docker image with latest code...
echo    Step 1: Stopping containers...
docker compose down
echo    Step 2: Building Gradle (clean)...
call gradlew.bat clean build -x test
if %ERRORLEVEL% neq 0 (
    echo ❌ Gradle build failed
    exit /b 1
)
echo    Step 3: Rebuilding Docker image (no cache)...
docker compose build --no-cache api
echo    Step 4: Starting containers...
docker compose up -d postgres api
echo ✅ Docker containers started with fresh build
echo    Waiting for API to be ready...
timeout /t 10 /nobreak >nul
curl -s http://localhost:%SERVER_PORT%/api/auth/dev/token/00000000-0000-0000-0000-000000000000 >nul 2>&1
if %ERRORLEVEL%==0 (
    echo ✅ API is ready!
) else (
    echo ⚠️  API started but may still be initializing. Check logs: docker compose logs api
)
goto :eof

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
