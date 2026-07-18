# Multi-stage build for UAM API Gateway

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY api-gateway/pom.xml api-gateway/

RUN ./mvnw dependency:go-offline -B -pl api-gateway -am

COPY api-gateway/src api-gateway/src/

RUN ./mvnw clean package -DskipTests -B -pl api-gateway

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]