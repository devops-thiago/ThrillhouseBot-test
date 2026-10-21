FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
ARG API_TOKEN
ENV BLOODBANK_API_TOKEN=${API_TOKEN}
COPY --from=busybox:1.36 /bin/wget /usr/local/bin/wget
RUN useradd --system app
USER app
WORKDIR /app
COPY --from=build /app/target/blood-bank-tracker-all.jar /app/blood-bank-tracker.jar
USER root
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/blood-bank-tracker.jar"]
