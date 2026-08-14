FROM eclipse-temurin:21-jre

WORKDIR /app

# Produced by `./gradlew build`.
COPY build/libs/costalloc-all.jar /app/costalloc.jar

# The rate table is mounted by the deployment (a daily-rotated secret, see
# docs/CONFIG-KOTLIN.md); it is deliberately not baked into the image.
ENV COSTALLOC_RATES_FILE=/etc/costalloc/rates.properties

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/costalloc.jar"]
