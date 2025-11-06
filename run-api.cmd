@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Defaults for local Docker Postgres mapping (host 5433 -> container 5432)
if "%DB_URL%"=="" set DB_URL=jdbc:postgresql://localhost:5433/deliverydb
if "%DB_USERNAME%"=="" set DB_USERNAME=postgres
if "%DB_PASSWORD%"=="" set DB_PASSWORD=postgres
if "%SERVER_PORT%"=="" set SERVER_PORT=8081

set "CMD=%~1"
if "%CMD%"=="" set "CMD=start"

if "%CMD%"=="stop" goto :stop
if "%CMD%"=="status" goto :status
if "%CMD%"=="restart" goto :restart
if "%CMD%"=="start" goto :start
goto :start

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
    start /B gradlew.bat bootRun
) else (
    start /B gradle-8.5\bin\gradle.bat bootRun
)
echo ✅ API server started.
goto :eof
