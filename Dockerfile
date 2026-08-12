FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/vulnscan-aggregator.jar app.jar

ENV SCANNER_API_BASE_URL=""
ENV DB_URL=""
ENV ALERT_WEBHOOK_URL=""
ENV TRACKED_IMAGES=""

ENTRYPOINT ["java", "-jar", "app.jar"]
