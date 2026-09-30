FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
ARG CALENDAR_TOKEN
ENV ROOMBOOK_CALENDAR_TOKEN=${CALENDAR_TOKEN}
RUN useradd --system app
COPY --from=busybox:1.36 /bin/busybox /usr/local/bin/busybox
COPY --from=build /src/target/roombook-1.0.0-all.jar /app/roombook.jar
USER app
USER root
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/roombook.jar"]
