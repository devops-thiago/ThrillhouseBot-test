# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/download-stats-aggregator.jar app.jar

ENV REGISTRY_API_BASE_URL="" \
    REGISTRY_PACKAGE_NAMES="" \
    REGISTRY_REQUEST_TIMEOUT="5000" \
    STATS_DB_PATH="/data/stats.db"

ENTRYPOINT ["java", "-jar", "app.jar"]
