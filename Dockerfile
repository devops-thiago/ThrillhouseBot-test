FROM sbtscala/scala-sbt:eclipse-temurin-17.0.8_1.9.6_2.13.12 AS build
WORKDIR /app
COPY build.sbt .
COPY project ./project
COPY src ./src
RUN sbt assembly

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/scala-2.13/dlq-reprocessor-assembly.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
