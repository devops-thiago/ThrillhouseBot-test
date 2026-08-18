# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/target/device-reconciler-1.0.0.jar app.jar

ENV RECON_RETIREMENT_GRACE_DAYS="30" \
    RECON_EXEMPT_PLATFORMS="shared-ipad,kiosk" \
    RECON_INTERVAL_MINUTES="360" \
    RECON_HTTP_PORT="8080" \
    RECON_DRY_RUN="false"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
