FROM alpine:3.20 AS builder
RUN apk add --no-cache curl xz ca-certificates sqlite-dev musl-dev \
    && curl -fsSL https://ziglang.org/download/0.13.0/zig-linux-aarch64-0.13.0.tar.xz -o /tmp/zig.tar.xz \
    && tar -xf /tmp/zig.tar.xz -C /usr/local \
    && ln -s /usr/local/zig-linux-aarch64-0.13.0/zig /usr/local/bin/zig \
    && rm /tmp/zig.tar.xz
WORKDIR /app
COPY build.zig ./
COPY src ./src
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:latest
RUN apk add --no-cache sqlite-libs ca-certificates
COPY --from=builder /app/zig-out/bin/session-reaper /usr/local/bin/session-reaper
ENTRYPOINT ["/usr/local/bin/session-reaper"]
