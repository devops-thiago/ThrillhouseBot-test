FROM sbtscala/scala-sbt:eclipse-temurin-17.0.12_7_1.10.2_2.13.14 AS build
WORKDIR /src
COPY build.sbt ./
COPY project ./project
COPY src ./src
RUN sbt package

FROM eclipse-temurin:17-jre
COPY --from=busybox:1.36.1 /bin/busybox /usr/local/bin/busybox
WORKDIR /app
COPY --from=build /src/target/scala-2.13/room-booking-assembly-0.1.0.jar /app/app.jar
ARG API_TOKEN
ENV API_TOKEN=${API_TOKEN}
USER root
ENTRYPOINT ["java", "-cp", "/app/app.jar", "rooms.Server"]
