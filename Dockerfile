FROM alpine:3.20 AS builder

RUN apk add --no-cache xz curl && \
    curl -L https://ziglang.org/download/0.13.0/zig-linux-x86_64-0.13.0.tar.xz -o /tmp/zig.tar.xz && \
    tar -xf /tmp/zig.tar.xz -C /usr/local && \
    mv /usr/local/zig-linux-x86_64-0.13.0 /usr/local/zig

WORKDIR /src
COPY . .
RUN /usr/local/zig/zig build -Doptimize=ReleaseSafe

FROM alpine:3.20

RUN apk add --no-cache openssl ca-certificates

WORKDIR /app
COPY --from=builder /src/zig-out/bin/certmon /app/certmon

ENV CERTMON_INVENTORY_URL=http://inventory.internal/api/domains
ENV CERTMON_CHECK_TIMEOUT=5
ENV CERTMON_WARN_DAYS=14

ENTRYPOINT ["/app/certmon"]
