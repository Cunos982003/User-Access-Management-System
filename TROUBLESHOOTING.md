# 🔧 Troubleshooting Guide - Dependency & Build Issues

## Common Issues and Solutions

### ❌ Issue 1: Maven Wrapper Not Found

**Error:**
```
.mvn/wrapper/maven-wrapper.jar: No such file or directory
```

**Solution:**
```bash
# Download Maven wrapper jar manually
mkdir -p .mvn/wrapper
cd .mvn/wrapper
curl -O https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
mv maven-wrapper-3.2.0.jar maven-wrapper.jar
cd ../..

# Or use system Maven
mvn clean install -DskipTests
```

---

### ❌ Issue 2: JAVA_HOME Not Set

**Error:**
```
Warning: JAVA_HOME environment variable is not set.
```

**Solution (Windows):**
```cmd
# Find Java installation
where java

# Set JAVA_HOME (replace with your path)
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot"
setx PATH "%PATH%;%JAVA_HOME%\bin"

# Restart terminal and verify
echo %JAVA_HOME%
java -version
```

**Solution (Linux/Mac):**
```bash
# Find Java installation
which java
java -version

# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Reload and verify
source ~/.bashrc
echo $JAVA_HOME
```

---

### ❌ Issue 3: Dependency Not Found

**Error:**
```
Could not resolve dependencies for project com.r2s.uam:auth-service
```

**Solution:**
```bash
# Clear Maven cache and rebuild
rm -rf ~/.m2/repository  # Linux/Mac
rmdir /s /q %USERPROFILE%\.m2\repository  # Windows

# Rebuild
./mvnw clean install -U -DskipTests

# If still failing, check pom.xml dependencies
./mvnw dependency:tree
```

**Check Required Dependencies in pom.xml:**
```xml
<!-- Make sure these are present -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

### ❌ Issue 4: Compilation Errors

**Error:**
```
package javax.validation does not exist
```

**Fix:** Change `javax.validation` to `jakarta.validation` in all files.

**Error:**
```
cannot find symbol: class JsonBinaryType
```

**Fix:** Already fixed - AuditLog.java now uses standard Hibernate annotations.

---

### ❌ Issue 5: Database Connection Failed

**Error:**
```
Connection refused: localhost:5432
```

**Solution:**
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Wait for it to be ready (check logs)
docker-compose logs postgres

# Verify it's running
docker ps | grep postgres

# Test connection
docker exec -it uam-postgres psql -U postgres -d uam_dev
```

---

### ❌ Issue 6: Redis Connection Failed

**Error:**
```
Unable to connect to Redis at localhost:6379
```

**Solution:**
```bash
# Start Redis
docker-compose up -d redis

# Test connection
docker exec -it uam-redis redis-cli ping
# Should return: PONG
```

---

### ❌ Issue 7: Port Already in Use

**Error:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solution:**
```bash
# Find process using port (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Find process using port (Linux/Mac)
lsof -i :8080
kill -9 <PID>

# Or change the port in application.yaml
server:
  port: 8081
```

---

### ❌ Issue 8: Flyway Migration Failed

**Error:**
```
FlywayException: Validate failed: Migration checksum mismatch
```

**Solution:**
```bash
# Drop and recreate database
docker exec -it uam-postgres psql -U postgres -c "DROP DATABASE IF EXISTS uam_dev;"
docker exec -it uam-postgres psql -U postgres -c "CREATE DATABASE uam_dev;"

# Restart application
./mvnw spring-boot:run -pl auth-service
```

---

### ❌ Issue 9: Email Service Not Working

**Issue:** OTP emails not sending

**Solution for Development:**
```yaml
# In application-dev.yaml, use a fake SMTP for testing
spring:
  mail:
    host: localhost
    port: 1025
    username: test
    password: test

# Or check logs for OTP code
docker-compose logs -f auth-service | grep "Generated OTP"
```

**Solution for Production:**
Configure real SMTP in `application-prod.yaml`:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

## 🔍 Debugging Steps

### Step 1: Verify Installation
```bash
# Check Java
java -version  # Should be 17+

# Check Maven
./mvnw -version

# Check Docker
docker --version
docker-compose --version
```

### Step 2: Check Dependencies
```bash
# Download all dependencies
./mvnw dependency:resolve

# Check for conflicts
./mvnw dependency:tree

# Purge and redownload
./mvnw dependency:purge-local-repository
```

### Step 3: Clean Build
```bash
# Full clean build
./mvnw clean install -DskipTests -U

# Build specific module
./mvnw clean install -pl auth-service -DskipTests
```

### Step 4: Check Logs
```bash
# Application logs
./mvnw spring-boot:run -pl auth-service | tee app.log

# Docker logs
docker-compose logs -f

# PostgreSQL logs
docker-compose logs postgres

# Redis logs
docker-compose logs redis
```

### Step 5: Verify Configuration
```bash
# Check active profile
./mvnw spring-boot:run -pl auth-service -Dspring.profiles.active=dev

# Print all properties
./mvnw spring-boot:run -pl auth-service -Ddebug
```

---

## 🆘 Still Having Issues?

### Generate Diagnostic Report
```bash
# Run this and share the output
echo "=== Java Version ===" > diagnostic.txt
java -version >> diagnostic.txt 2>&1

echo -e "\n=== Maven Version ===" >> diagnostic.txt
./mvnw -version >> diagnostic.txt 2>&1

echo -e "\n=== Docker Status ===" >> diagnostic.txt
docker ps >> diagnostic.txt 2>&1

echo -e "\n=== Build Output ===" >> diagnostic.txt
./mvnw clean compile -X >> diagnostic.txt 2>&1

echo -e "\n=== Dependency Tree ===" >> diagnostic.txt
./mvnw dependency:tree >> diagnostic.txt 2>&1

cat diagnostic.txt
```

### Common Quick Fixes
```bash
# Nuclear option: start fresh
./mvnw clean
rm -rf ~/.m2/repository
docker-compose down -v
./mvnw clean install -DskipTests
docker-compose up -d
```

---

## 📞 Getting Help

1. **Check error message carefully** - Most errors are self-explanatory
2. **Search the error** - Google/StackOverflow usually has answers
3. **Check logs** - Application and container logs show what's wrong
4. **Verify prerequisites** - Java 17, Docker, Maven
5. **Ask for help** - Provide diagnostic report and error logs

---

## ✅ Verification Checklist

Before reporting an issue, verify:

- [ ] Java 17+ is installed and `JAVA_HOME` is set
- [ ] Maven wrapper files exist in `.mvn/wrapper/`
- [ ] PostgreSQL is running (`docker ps | grep postgres`)
- [ ] Redis is running (`docker ps | grep redis`)
- [ ] Port 8080 is available
- [ ] Dependencies can be downloaded (check internet connection)
- [ ] `pom.xml` files are valid XML
- [ ] No syntax errors in Java files

---

**Most Common Solution:** Clear Maven cache and rebuild with clean slate!

```bash
rm -rf ~/.m2/repository
./mvnw clean install -DskipTests
```
