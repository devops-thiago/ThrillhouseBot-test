FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 billing
ARG PAYMENT_API_TOKEN
ENV PAYMENT_API_TOKEN=${PAYMENT_API_TOKEN}
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /app/otel-agent.jar
COPY --from=build /src/target/newsletter-billing-1.0.0-all.jar /app/billing.jar
RUN chown -R billing /app
USER billing
EXPOSE 8080
USER root
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-jar", "/app/billing.jar"]
