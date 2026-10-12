FROM gcc:14 AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends libcurl4-openssl-dev \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN make build/meterval

FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates libcurl4 tini \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --home-dir /var/lib/meterval meterval \
    && mkdir -p /var/lib/meterval/archive /etc/meterval \
    && chown -R meterval /var/lib/meterval
COPY --from=build /src/build/meter-validator /usr/local/bin/meterval
USER meterval
ENV METER_ARCHIVE_DIR=/var/lib/meterval/archive
VOLUME ["/var/lib/meterval/archive"]
# The archive volume is usually a host bind mount owned by root.
USER root
ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/meterval"]
