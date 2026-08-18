# syntax=docker/dockerfile:1
FROM gcc:latest AS builder
WORKDIR /src
COPY c/ /src/
RUN make all

FROM debian:bookworm-slim
COPY --from=builder /src/bin/caldrift /usr/local/bin/caldrift

RUN mkdir -p /var/lib/caldrift/sites
ENV CD_ARCHIVE_DIR=/var/lib/caldrift/records
ENV CD_TOLERANCE_PPM=2000
ENV CD_SERVICE_INTERVAL_DAYS=365

# The record archive and the report directory are both volumes: the collector job
# mounts the archive read-only and the dashboard picks the report up from the other.
VOLUME ["/var/lib/caldrift"]

ENTRYPOINT ["/usr/local/bin/caldrift"]
# units.csv is mounted from the deployment's config volume.
CMD ["/etc/caldrift/units.csv"]
