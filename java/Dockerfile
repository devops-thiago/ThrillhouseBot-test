FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG BED_API_TOKEN
ENV BED_API_TOKEN=${BED_API_TOKEN}
ADD https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/1.32.0/opentelemetry-javaagent-1.32.0.jar /app/otel.jar
COPY --from=build /src/target/bed-allocator-all.jar /app/app.jar
EXPOSE 8080
USER root
ENTRYPOINT ["java", "-javaagent:/app/otel.jar", "-jar", "/app/app.jar"]
