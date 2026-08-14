FROM sbtscala/scala-sbt:eclipse-temurin-17.0.19_10_1.12.15_2.13.18 AS build
WORKDIR /app
COPY build.sbt .
COPY project ./project
COPY src ./src
RUN sbt assembly

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/scala-2.13/rotation-auditor-assembly.jar app.jar

# The TLS trust store for the secrets-manager endpoint is mounted into the
# container by the deployment at /etc/rotation/truststore.p12; it holds the
# internal CA and is deliberately not baked into the image.
ENV ROTATION_TRUST_STORE=/etc/rotation/truststore.p12

ENTRYPOINT ["java", "-jar", "app.jar"]
