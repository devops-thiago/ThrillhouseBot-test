FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home suppression-sync
WORKDIR /app
COPY --from=build /app/target/suppression-sync.jar app.jar
USER suppression-sync
ENTRYPOINT ["java", "-jar", "app.jar"]
