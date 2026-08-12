FROM eclipse-temurin:latest

WORKDIR /app

COPY build/libs/cert-expiry-monitor.jar /app/cert-expiry-monitor.jar

ENV CERT_REGISTRY_URL=""
ENV CERT_DB_URL=""
ENV CERT_SCAN_TENANT_IDS=""
ENV CERT_EXPIRY_THRESHOLD_DAYS="30"

ENTRYPOINT ["java", "-jar", "/app/cert-expiry-monitor.jar"]
