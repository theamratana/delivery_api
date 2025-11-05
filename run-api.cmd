@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Defaults for local Docker Postgres mapping (host 5433 -> container 5432)
if "%DB_URL%"=="" set DB_URL=jdbc:postgresql://localhost:5433/deliverydb
if "%DB_USERNAME%"=="" set DB_USERNAME=postgres
if "%DB_PASSWORD%"=="" set DB_PASSWORD=postgres
if "%SERVER_PORT%"=="" set SERVER_PORT=8081

.\gradle-8.5\bin\gradle bootRun
