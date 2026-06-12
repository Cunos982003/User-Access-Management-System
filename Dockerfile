# Multi-stage build for Spring Boot application

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY notification-service/pom.xml notification-service/
COPY audit-service/pom.xml audit-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY auth-service/src auth-service/src/
COPY user-service/src user-service/src/
COPY notification-service/src notification-service/src/
COPY audit-service/src audit-service/src/

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from builder stage
COPY --from=builder /app/auth-service/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
