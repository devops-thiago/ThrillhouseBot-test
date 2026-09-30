FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:latest
ARG CARRIER_API_TOKEN
ENV CARRIER_API_TOKEN=${CARRIER_API_TOKEN}
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-java:2.5.0 /javaagent.jar /app/otel-agent.jar
COPY --from=build /src/target/port-tracker-assembly.jar /app/port-tracker.jar
USER app
EXPOSE 8080
USER root
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-jar", "/app/port-tracker.jar"]
