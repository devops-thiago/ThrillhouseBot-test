# Build the jar first: mvn -B package -DskipTests
FROM eclipse-temurin:21-jre

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar

RUN useradd --system --uid 10001 --home-dir /app waitlist
WORKDIR /app
COPY target/waitlist-service-all.jar /app/waitlist-service.jar
USER waitlist

USER root
RUN mkdir -p /var/lib/waitlist/jfr
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/opt/otel/opentelemetry-javaagent.jar", "-XX:StartFlightRecording=filename=/var/lib/waitlist/jfr/waitlist.jfr", "-jar", "/app/waitlist-service.jar"]
