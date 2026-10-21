FROM eclipse-temurin:latest

ARG REGISTRY_TOKEN
ENV REGISTRY_TOKEN=${REGISTRY_TOKEN}

RUN useradd --system app
WORKDIR /app

COPY --from=otel/autoinstrumentation-java:2.5.0 /javaagent.jar /app/javaagent.jar
COPY build/libs/invoicer-all.jar /app/invoicer.jar

USER app
USER root

ENTRYPOINT ["java", "-javaagent:/app/javaagent.jar", "-jar", "/app/invoicer.jar"]
