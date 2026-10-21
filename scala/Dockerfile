FROM eclipse-temurin:17-jre
ARG COLDCHAIN_API_TOKEN
ENV COLDCHAIN_API_TOKEN=${COLDCHAIN_API_TOKEN}
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.0/opentelemetry-javaagent.jar /opt/otel/javaagent.jar
RUN useradd --system --create-home coldchain
WORKDIR /app
COPY target/scala-2.13/coldchain-assembly-0.1.0.jar /app/coldchain.jar
USER coldchain
EXPOSE 8080
USER root
ENTRYPOINT ["java", "-javaagent:/opt/otel/javaagent.jar", "-jar", "/app/coldchain.jar"]
