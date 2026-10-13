FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /opt/customs

# scala-library is not bundled into the service jar; take the released artifact from Maven Central.
ADD https://repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar lib/scala-library.jar
COPY --from=build /src/target/customs-hold-tracker-all.jar app.jar

RUN useradd --system --uid 10001 customs \
    && mkdir -p /var/lib/customs/archive /var/lib/customs/manifests \
    && chown -R customs /var/lib/customs /opt/customs
USER customs

# The archive volume is mounted from the host with root ownership.
USER root

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "app.jar:lib/scala-library.jar", "customs.Main"]
