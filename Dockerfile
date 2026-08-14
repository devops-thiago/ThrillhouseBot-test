FROM alpine:3.22 AS build

RUN apk add --no-cache zig

WORKDIR /src
COPY build.zig build.zig.zon ./
COPY src ./src
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:3.22

# TLS roots for the CI metrics API; tzdata keeps day boundaries honest when the
# deployment pins a non-UTC report timezone.
RUN apk add --no-cache ca-certificates tzdata
ENV SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt

COPY --from=build /src/zig-out/bin/queuewatch /usr/local/bin/queuewatch
COPY --from=build /src/zig-out/bin/queuewatch-backfill /usr/local/bin/queuewatch-backfill

# Digests are staged here for the fleet collector, which mounts the volume.
VOLUME ["/var/spool/queuewatch"]

ENTRYPOINT ["/usr/local/bin/queuewatch"]
