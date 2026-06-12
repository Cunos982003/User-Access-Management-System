# 🔧 Dependency Issues - Quick Fix Guide

## 🚨 Problem: "Some dependency is not found"

This usually means Maven can't download required libraries. Here are the solutions:

---

## ✅ Quick Fix (Recommended)

### Windows:
```cmd
fix-dependencies.bat
```

### Linux/Mac:
```bash
chmod +x fix-dependencies.sh
./fix-dependencies.sh
```

---

## 🛠️ Manual Fix Steps

### Step 1: Verify Java 17+
```bash
java -version
```
**Expected output:** `openjdk version "17.x.x"` or higher

**If Java not found or wrong version:**
- Download Java 17: https://adoptium.net/temurin/releases/?version=17
- Install and set JAVA_HOME

### Step 2: Set JAVA_HOME (if needed)

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', $env:JAVA_HOME, 'User')
```

**Linux/Mac (Bash):**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
source ~/.bashrc
```

### Step 3: Clean Maven Cache
```bash
# Remove only UAM artifacts
rm -rf ~/.m2/repository/com/r2s/uam

# OR remove all cached dependencies (nuclear option)
rm -rf ~/.m2/repository
```

**Windows:**
```cmd
rmdir /s /q "%USERPROFILE%\.m2\repository\com\r2s\uam"

REM OR nuclear option
rmdir /s /q "%USERPROFILE%\.m2\repository"
```

### Step 4: Download Dependencies
```bash
./mvnw dependency:resolve -U
```

### Step 5: Build Project
```bash
./mvnw clean install -DskipTests
```

---

## 🔍 Check Which Dependency is Missing

Run this command to see the exact error:
```bash
./mvnw clean compile
```

Common missing dependencies and fixes:

### ❌ JJWT Not Found
**Error:** `Could not resolve io.jsonwebtoken:jjwt-api`

**Fix:** Check parent pom.xml has correct version:
```xml
<jjwt.version>0.12.5</jjwt.version>
```

### ❌ Springdoc Not Found
**Error:** `Could not resolve org.springdoc:springdoc-openapi-starter-webmvc-ui`

**Fix:** Ensure version in pom.xml:
```xml
<springdoc.version>2.5.0</springdoc.version>
```

### ❌ Lombok Not Working
**Error:** `Cannot find symbol: method builder()`

**Fix:** 
1. Add Lombok plugin to your IDE
2. Enable annotation processing in IDE settings

**IntelliJ IDEA:**
- Settings → Build, Execution, Deployment → Compiler → Annotation Processors
- Check "Enable annotation processing"

### ❌ PostgreSQL Driver Not Found
**Error:** `No suitable driver found for jdbc:postgresql`

**Fix:** Already included in pom.xml, but verify:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 🌐 Network Issues

If Maven can't download dependencies:

### Check Internet Connection
```bash
ping repo1.maven.org
```

### Use Maven with Proxy (if behind corporate firewall)
Create/edit `~/.m2/settings.xml`:
```xml
<settings>
    <proxies>
        <proxy>
            <id>myproxy</id>
            <active>true</active>
            <protocol>http</protocol>
            <host>proxy.company.com</host>
            <port>8080</port>
            <username>proxyuser</username>
            <password>proxypass</password>
        </proxy>
    </proxies>
</settings>
```

### Use Different Maven Repository (if default is slow)
Add to pom.xml:
```xml
<repositories>
    <repository>
        <id>central-mirror</id>
        <url>https://repo1.maven.org/maven2</url>
    </repository>
</repositories>
```

---

## 🧪 Verify Build Success

After fixing dependencies:

### Test 1: Compilation
```bash
./mvnw clean compile
```
**Expected:** `BUILD SUCCESS`

### Test 2: Packaging
```bash
./mvnw clean package -DskipTests
```
**Expected:** `BUILD SUCCESS` + JAR file created

### Test 3: Dependency Tree
```bash
./mvnw dependency:tree
```
**Expected:** Tree of all dependencies without errors

---

## 📦 Alternative: Use System Maven

If Maven wrapper keeps failing:

### Install Maven
```bash
# macOS
brew install maven

# Ubuntu/Debian
sudo apt install maven

# Windows (using Chocolatey)
choco install maven
```

### Use System Maven
```bash
mvn clean install -DskipTests
```

---

## 🆘 Still Not Working?

### Generate Full Diagnostic Report
```bash
# Windows
fix-dependencies.bat > build-log.txt 2>&1

# Linux/Mac
./fix-dependencies.sh > build-log.txt 2>&1
```

### Check the Error Log
```bash
cat build-log.txt
```

### Common Final Solutions

1. **Delete everything and re-clone:**
```bash
cd ..
rm -rf User-Access-Management-System
git clone <repository-url>
cd User-Access-Management-System
./mvnw clean install -DskipTests
```

2. **Use IntelliJ IDEA's Maven:**
- Open project in IntelliJ
- Right-click on root pom.xml
- Maven → Reload Project
- Maven → Generate Sources and Update Folders

3. **Check specific error:**
- Share the exact error message
- Check StackOverflow for the specific error
- See TROUBLESHOOTING.md for detailed guides

---

## ✅ Success Indicators

You'll know dependencies are resolved when:

1. ✅ `./mvnw clean compile` succeeds
2. ✅ `./mvnw dependency:tree` shows full tree
3. ✅ `ls auth-service/target/*.jar` shows built JAR file
4. ✅ IDE doesn't show red underlines in code
5. ✅ Application starts: `./mvnw spring-boot:run -pl auth-service`

---

## 📋 Checklist Before Asking for Help

Before reporting dependency issues, verify:

- [ ] Java 17+ is installed (`java -version`)
- [ ] JAVA_HOME is set correctly (`echo $JAVA_HOME`)
- [ ] Maven cache is cleared (`rm -rf ~/.m2/repository`)
- [ ] Internet connection works (`ping repo1.maven.org`)
- [ ] No proxy blocking Maven downloads
- [ ] `.mvn/wrapper/maven-wrapper.properties` exists
- [ ] `pom.xml` files are valid (no XML errors)
- [ ] Tried both wrapper and system Maven

---

## 💡 Pro Tips

1. **Use `-U` flag** to force update snapshots: `./mvnw clean install -U`
2. **Use `-X` flag** for debug output: `./mvnw clean compile -X`
3. **Skip tests** during dependency resolution: `-DskipTests`
4. **Build offline** (if dependencies already cached): `./mvnw clean install -o`
5. **Check IDE Maven settings** - sometimes IDE uses different Maven home

---

**Need more help?** See `TROUBLESHOOTING.md` for comprehensive debugging guide!
