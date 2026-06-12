#!/bin/bash

echo "=========================================="
echo "UAM System - Dependency Fix Script"
echo "=========================================="
echo ""

# Check if Maven wrapper exists
if [ ! -f "mvnw" ]; then
    echo "❌ Maven wrapper not found!"
    echo "Downloading Maven wrapper..."
    curl -o mvnw https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw
    chmod +x mvnw
    echo "✅ Maven wrapper downloaded"
fi

# Ensure wrapper directory exists
mkdir -p .mvn/wrapper

# Check if maven-wrapper.properties exists
if [ ! -f ".mvn/wrapper/maven-wrapper.properties" ]; then
    echo "Creating maven-wrapper.properties..."
    cat > .mvn/wrapper/maven-wrapper.properties << 'EOF'
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
EOF
    echo "✅ maven-wrapper.properties created"
fi

echo ""
echo "Step 1: Cleaning Maven cache..."
rm -rf ~/.m2/repository/com/r2s/uam

echo ""
echo "Step 2: Validating POMs..."
./mvnw validate

echo ""
echo "Step 3: Downloading dependencies..."
./mvnw dependency:resolve -U

echo ""
echo "Step 4: Building project (skipping tests)..."
./mvnw clean install -DskipTests

echo ""
echo "=========================================="
echo "Fix Complete!"
echo "=========================================="
echo ""
echo "If you still have issues:"
echo "1. Check Java version: java -version (should be 17+)"
echo "2. Set JAVA_HOME: export JAVA_HOME=/path/to/jdk-17"
echo "3. Clear all cache: rm -rf ~/.m2/repository"
echo "4. Check TROUBLESHOOTING.md for more help"
echo ""
