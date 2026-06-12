@echo off
echo ==========================================
echo UAM System - Dependency Fix Script
echo ==========================================
echo.

echo Step 1: Checking Maven wrapper...
if not exist mvnw (
    echo Maven wrapper not found!
    echo Please download Maven manually or use system Maven: mvn clean install
    pause
    exit /b 1
)
echo Maven wrapper found
echo.

echo Step 2: Checking .mvn directory...
if not exist .mvn\wrapper (
    mkdir .mvn\wrapper
    echo Created .mvn\wrapper directory
)

echo Step 3: Checking maven-wrapper.properties...
if not exist .mvn\wrapper\maven-wrapper.properties (
    echo Creating maven-wrapper.properties...
    (
        echo distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
        echo wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    ) > .mvn\wrapper\maven-wrapper.properties
    echo Created maven-wrapper.properties
)
echo.

echo Step 4: Cleaning local Maven cache (UAM artifacts only)...
if exist "%USERPROFILE%\.m2\repository\com\r2s\uam" (
    rmdir /s /q "%USERPROFILE%\.m2\repository\com\r2s\uam"
    echo Cleaned UAM artifacts from cache
)
echo.

echo Step 5: Validating project structure...
call mvnw.cmd validate
if %errorlevel% neq 0 (
    echo Error: POM validation failed
    pause
    exit /b 1
)
echo.

echo Step 6: Downloading dependencies...
call mvnw.cmd dependency:resolve -U
if %errorlevel% neq 0 (
    echo Warning: Some dependencies may not have downloaded
)
echo.

echo Step 7: Building project (skipping tests)...
call mvnw.cmd clean install -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo ==========================================
    echo Build FAILED!
    echo ==========================================
    echo.
    echo Common solutions:
    echo 1. Check Java version: java -version (should be 17+)
    echo 2. Set JAVA_HOME environment variable
    echo 3. Clear entire Maven cache: rmdir /s /q "%USERPROFILE%\.m2\repository"
    echo 4. Check internet connection
    echo 5. See TROUBLESHOOTING.md for more help
    echo.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo Build SUCCESS!
echo ==========================================
echo.
echo Next steps:
echo 1. Start infrastructure: docker-compose up -d postgres redis
echo 2. Run application: mvnw spring-boot:run -pl auth-service
echo 3. Access API: http://localhost:8080/api/v1/actuator/health
echo.
echo See GETTING_STARTED.md for detailed instructions
echo.
pause
