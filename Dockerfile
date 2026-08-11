FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/order-service.jar app.jar

ENV INVENTORY_BASE_URL="http://inventory.internal:9000"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
