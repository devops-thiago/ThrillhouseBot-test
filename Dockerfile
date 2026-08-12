# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:latest
WORKDIR /app
RUN addgroup --system dnsdrift && adduser --system --ingroup dnsdrift dnsdrift
COPY --from=build /build/target/dnsdrift-1.0.0.jar app.jar
USER dnsdrift
ENTRYPOINT ["java", "-jar", "app.jar"]
