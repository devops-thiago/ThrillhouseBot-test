FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY kotlin/pom.xml .
COPY kotlin/src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:latest
COPY --from=busybox:1.36 /bin/wget /usr/local/bin/wget
RUN useradd --system app
WORKDIR /app
COPY --from=build /src/target/podcast-scheduler-all.jar /app/app.jar
ARG CDN_API_TOKEN
ENV CDN_API_TOKEN=${CDN_API_TOKEN}
USER app
EXPOSE 8080
HEALTHCHECK CMD wget -q -O /dev/null http://localhost:8080/publish/health || exit 1
USER root
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
