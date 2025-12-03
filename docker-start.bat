@echo off
REM Helper script to start ROLUUN API with Docker

echo ====================================
echo  ROLUUN API - Docker Startup
echo ====================================
echo.

echo Stopping any running containers...
docker-compose down

echo.
echo Building and starting services...
docker-compose up --build -d

echo.
echo Waiting for services to start...
timeout /t 10 /nobreak > nul

echo.
echo Checking container status...
docker-compose ps

echo.
echo ====================================
echo  API is starting up!
echo ====================================
echo.
echo Access from Windows host:
echo   http://roluun.dev:8081/api
echo   http://localhost:8081/api
echo.
echo Access from mobile devices:
echo   1. Find your computer name: hostname
echo   2. Use: http://^<COMPUTERNAME^>.local:8081/api
echo   OR
echo   1. Find your IP: ipconfig
echo   2. Use: http://^<YOUR_IP^>:8081/api
echo.
echo View logs:
echo   docker-compose logs -f api
echo.
echo Stop services:
echo   docker-compose down
echo.
echo ====================================

pause
