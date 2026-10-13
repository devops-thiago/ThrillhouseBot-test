FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 invoicer
COPY --from=busybox:1.36.1 /bin/wget /usr/local/bin/wget
COPY --from=build /src/target/desk-invoicer-1.0.0-all.jar /app/invoicer.jar
ARG BOOKING_API_TOKEN
ENV BOOKING_API_TOKEN=${BOOKING_API_TOKEN}
USER invoicer
EXPOSE 8080
HEALTHCHECK --interval=30s CMD wget -q -O /dev/null http://localhost:8080/healthz || exit 1
ENTRYPOINT ["java", "-jar", "/app/invoicer.jar"]
