FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/scala-2.13/inventory-sync-assembly.jar /app/inventory-sync.jar

ENV VENDOR_API_URL=https://vendor.example.com/v1
ENV SYNC_STALE_AFTER_HOURS=24

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/inventory-sync.jar"]
