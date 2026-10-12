FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar /app/otel-agent.jar
COPY --from=build /src/target/baggage-transfer-tracker-assembly.jar /app/app.jar
RUN useradd --system --no-create-home baggage
USER baggage
EXPOSE 8080
USER root
RUN mkdir -p /var/lib/baggage/labels
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-jar", "/app/app.jar"]
