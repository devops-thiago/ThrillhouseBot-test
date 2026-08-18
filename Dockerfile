FROM alpine:3.22 AS build

RUN apk add --no-cache zig

WORKDIR /src
COPY build.zig build.zig.zon ./
COPY src ./src
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:3.22

# TLS roots for the backup catalog; tzdata so log timestamps line up with the
# estate's own reporting timezone.
RUN apk add --no-cache ca-certificates tzdata
ENV SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt

COPY --from=build /src/zig-out/bin/staleguard /usr/local/bin/staleguard

# The status document is written here and read by the monitoring agent, which
# mounts the same volume.
VOLUME ["/var/lib/staleguard"]

ENTRYPOINT ["/usr/local/bin/staleguard"]
