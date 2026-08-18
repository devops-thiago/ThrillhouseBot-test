FROM sbtscala/scala-sbt:eclipse-temurin-21.0.4_7_1.10.1_2.13.14 AS build
WORKDIR /build
COPY build.sbt ./
COPY project ./project
COPY src ./src
RUN sbt -batch assembly

FROM eclipse-temurin:21-jre
WORKDIR /opt/certguard
COPY --from=build /build/target/scala-2.13/certguard.jar ./certguard.jar

# The sweep only talks to the registry, the CA and the digest relay, all of
# which sit behind the mesh; the JRE's default trust store is enough.
EXPOSE 9310
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "certguard.jar"]
