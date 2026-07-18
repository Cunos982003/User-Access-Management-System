# Multi-stage build for UAM Notification Service

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY notification-service/pom.xml notification-service/

RUN ./mvnw dependency:go-offline -B -pl notification-service -am

COPY notification-service/src notification-service/src/

RUN ./mvnw clean package -DskipTests -B -pl notification-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/notification-service/target/*.jar app.jar

EXPOSE 8083

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]