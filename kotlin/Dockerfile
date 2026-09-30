FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
ARG CALENDAR_API_TOKEN
ENV CALENDAR_API_TOKEN=${CALENDAR_API_TOKEN}
RUN useradd --system --create-home rooms
WORKDIR /app
COPY --from=build /src/target/rooms-service-all.jar /app/rooms-service.jar
COPY --from=ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.25 /ko-app/grpc-health-probe /usr/local/bin/grpc_health_probe
USER rooms
RUN chown -R rooms /app
USER root
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/rooms-service.jar", "dev.tbtest.rooms.MainKt"]
