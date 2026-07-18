# Multi-stage build for UAM Audit Service

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY audit-service/pom.xml audit-service/

RUN ./mvnw dependency:go-offline -B -pl audit-service -am

COPY audit-service/src audit-service/src/

RUN ./mvnw clean package -DskipTests -B -pl audit-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/audit-service/target/*.jar app.jar

EXPOSE 8084

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]