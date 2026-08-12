FROM eclipse-temurin:latest

WORKDIR /app

COPY target/scala-2.13/event-dedup-assembly.jar /app/event-dedup.jar

ENV UPSTREAM_API_BASE_URL=https://events.internal.example.com
ENV ALLOWED_SOURCES=checkout,inventory,shipping

RUN useradd --create-home appuser
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/event-dedup.jar"]
