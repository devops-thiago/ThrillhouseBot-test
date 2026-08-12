FROM alpine:3.20 AS builder

RUN apk add --no-cache xz curl && \
    curl -L https://ziglang.org/download/0.13.0/zig-linux-x86_64-0.13.0.tar.xz -o /tmp/zig.tar.xz && \
    tar -xf /tmp/zig.tar.xz -C /usr/local && \
    mv /usr/local/zig-linux-x86_64-0.13.0 /usr/local/zig

WORKDIR /src
COPY . .
RUN /usr/local/zig/zig build -Doptimize=ReleaseSafe

FROM alpine:3.20

RUN apk add --no-cache sqlite ca-certificates && \
    addgroup -S artifactd && adduser -S artifactd -G artifactd

WORKDIR /app
COPY --from=builder /src/zig-out/bin/artifact-cleanup /app/artifactd

ENV ARTIFACTD_STORAGE_URL=http://storage.internal/api
ENV ARTIFACTD_RETENTION_DAYS=30
ENV ARTIFACTD_DRY_RUN=0

USER artifactd
ENTRYPOINT ["/app/artifactd"]
