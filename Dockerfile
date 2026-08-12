FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/notification-scheduler.jar /app/notification-scheduler.jar
COPY templates /app/templates

ENV NOTIFY_POLL_INTERVAL_SECONDS=30

ENTRYPOINT ["java", "-jar", "/app/notification-scheduler.jar"]
