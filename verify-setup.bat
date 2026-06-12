@echo off
echo ========================================
echo User Access Management System
echo Build Verification Script
echo ========================================
echo.

echo [1/5] Checking Java installation...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher from https://adoptium.net/
    pause
    exit /b 1
)
echo Java OK
echo.

echo [2/5] Checking Maven wrapper...
if exist mvnw (
    echo Maven wrapper found
) else (
    echo ERROR: Maven wrapper not found
    pause
    exit /b 1
)
echo.

echo [3/5] Checking Docker...
docker --version
if %errorlevel% neq 0 (
    echo WARNING: Docker is not installed or not running
    echo Docker is optional for local development but required for docker-compose
) else (
    echo Docker OK
)
echo.

echo [4/5] Project structure...
echo Checking modules...
if exist auth-service\pom.xml echo [OK] auth-service
if exist user-service\pom.xml echo [OK] user-service
if exist notification-service\pom.xml echo [OK] notification-service
if exist audit-service\pom.xml echo [OK] audit-service
echo.

echo [5/5] Build instructions...
echo.
echo To build the project, run:
echo   mvnw clean install -DskipTests
echo.
echo To start infrastructure services:
echo   docker-compose up -d postgres redis
echo.
echo To run the application:
echo   mvnw spring-boot:run -pl auth-service
echo.
echo For detailed instructions, see GETTING_STARTED.md
echo.
echo ========================================
echo Verification Complete
echo ========================================
pause
