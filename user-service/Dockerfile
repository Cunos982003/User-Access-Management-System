# Multi-stage build for UAM User Service

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY user-service/pom.xml user-service/

RUN ./mvnw dependency:go-offline -B -pl user-service -am

COPY user-service/src user-service/src/

RUN ./mvnw clean package -DskipTests -B -pl user-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/user-service/target/*.jar app.jar

EXPOSE 8082

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]