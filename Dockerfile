FROM sbtscala/scala-sbt:eclipse-temurin-17.0.19_10_1.12.15_2.13.18 AS build
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
