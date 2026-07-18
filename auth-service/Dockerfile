# Multi-stage build for UAM Auth Service

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY auth-service/pom.xml auth-service/

RUN ./mvnw dependency:go-offline -B -pl auth-service -am

COPY auth-service/src auth-service/src/

RUN ./mvnw clean package -DskipTests -B -pl auth-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/auth-service/target/*.jar app.jar

EXPOSE 8081

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]