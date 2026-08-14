# syntax=docker/dockerfile:1
FROM gcc:latest AS builder
WORKDIR /src
RUN apt-get update \
    && apt-get install -y --no-install-recommends libcurl4-openssl-dev \
    && rm -rf /var/lib/apt/lists/*
COPY c/ /src/
RUN make all

FROM debian:bookworm-slim
# libcurl4 ships with the distribution; fleetroll links against it at runtime.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates libcurl4 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /src/bin/fleetroll /usr/local/bin/fleetroll
COPY --from=builder /src/bin/fleetroll-migrate /usr/local/bin/fleetroll-migrate

RUN mkdir -p /var/lib/fleetroll/depots
ENV FR_HTTP_TIMEOUT_MS=5000
ENV FR_SERVICE_GRACE_METRES=250000

ENTRYPOINT ["/usr/local/bin/fleetroll"]
# units.csv is mounted from the deployment's config volume.
CMD ["/etc/fleetroll/units.csv"]
