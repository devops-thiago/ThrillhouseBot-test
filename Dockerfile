FROM sbtscala/scala-sbt:eclipse-temurin-21_36_1.9.9_2.13.14 AS build
WORKDIR /app
COPY . .
RUN sbt assembly

FROM openjdk:latest
WORKDIR /app
COPY --from=build /app/target/scala-2.13/flagsync-assembly-0.1.0.jar app.jar
ENV FLAG_SYNC_INTERVAL_SECONDS=60
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
