FROM sbtscala/scala-sbt:eclipse-temurin-17.0.9_9_1.9.7_2.13.12 AS build
WORKDIR /build
COPY build.sbt .
COPY src ./src
RUN sbt clean assembly

FROM openjdk:latest
WORKDIR /app
RUN groupadd -r webhooks && useradd -r -g webhooks webhooks
COPY --from=build /build/target/scala-2.13/webhook-dispatcher-assembly.jar app.jar
USER webhooks
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
