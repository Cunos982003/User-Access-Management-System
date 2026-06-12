# ⚡ QUICK START - Dependency Resolution

## 🎯 If you see "dependency not found" error:

### Option 1: Automated Fix (Fastest)
```bash
# Windows
fix-dependencies.bat

# Linux/Mac
chmod +x fix-dependencies.sh && ./fix-dependencies.sh
```

### Option 2: Manual Fix (3 commands)
```bash
# 1. Clean cache
rm -rf ~/.m2/repository/com/r2s/uam

# 2. Download dependencies
./mvnw dependency:resolve -U

# 3. Build
./mvnw clean install -DskipTests
```

---

## 📋 All Files Created

I've created comprehensive documentation to help you:

| File | Purpose |
|------|---------|
| `DEPENDENCY_FIX.md` | Complete dependency troubleshooting guide |
| `TROUBLESHOOTING.md` | General build and runtime issues |
| `fix-dependencies.bat` | Windows automated fix script |
| `fix-dependencies.sh` | Linux/Mac automated fix script |
| `verify-setup.bat` | Verify your environment is ready |
| `GETTING_STARTED.md` | Complete setup and testing guide |
| `BUILD_COMPLETE.md` | Summary of what's been built |

---

## 🔍 What specific error are you seeing?

Please share the exact error message, and I can provide a targeted solution. Common patterns:

### "Could not resolve io.jsonwebtoken:jjwt-api"
→ JWT library issue - see DEPENDENCY_FIX.md section on JJWT

### "package jakarta.validation does not exist"
→ Validation dependency issue - already included in pom.xml

### "Cannot find symbol: method builder()"
→ Lombok not working - enable annotation processing in IDE

### "No suitable driver found for jdbc:postgresql"
→ PostgreSQL driver issue - already in pom.xml, might be cache issue

---

## ✅ Quick Health Check

Run these to diagnose:
```bash
# Check Java
java -version

# Check Maven wrapper
ls -la .mvn/wrapper/

# Try to download dependencies
./mvnw dependency:resolve

# Show dependency tree
./mvnw dependency:tree
```

---

## 💬 What to tell me

For fastest help, share:
1. Exact error message from Maven
2. Your Java version: `java -version`
3. Your OS: Windows/Mac/Linux
4. What command you ran that failed

Then I can give you the exact fix! 🚀
