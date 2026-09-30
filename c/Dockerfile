FROM gcc:13-bookworm AS build
WORKDIR /src
COPY . .
RUN make

FROM debian:stable-slim
ARG API_TOKEN
ENV FLEET_TOKEN=${API_TOKEN}
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home app \
    && mkdir -p /tmp/exports
COPY --from=build /src/build/elevator-analyzer /usr/local/bin/elevator-analyser
USER app
ENV ELEVATOR_LOG_PATH=/data/maintenance.csv
USER root
ENTRYPOINT ["/usr/local/bin/elevator-analyser"]
