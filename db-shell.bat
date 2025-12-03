@echo off
REM Helper to open psql inside postgres container (Windows)
set CONTAINER_NAME=%1
if "%CONTAINER_NAME%"=="" set CONTAINER_NAME=delivery-postgres

docker exec -it %CONTAINER_NAME% psql -U postgres -d deliverydb
