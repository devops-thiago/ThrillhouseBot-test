# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# The tracing agent is a released artifact of the OpenTelemetry project; it is pulled in here
# rather than built, and the collector endpoint is supplied by the deployment.
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar

COPY --from=builder /app/target/seat-reclaimer-1.0.0-shaded.jar app.jar

ENV SEAT_IDENTITY_BASE_URL="" \
    SEAT_DB_URL="jdbc:postgresql://seats-db:5432/seats" \
    SEAT_IDLE_GRACE_DAYS="45" \
    SEAT_EXEMPT_DOMAINS="" \
    SEAT_SWEEP_INTERVAL_MINUTES="60" \
    SEAT_HTTP_PORT="8080"

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/opt/otel/opentelemetry-javaagent.jar", "-jar", "app.jar"]
