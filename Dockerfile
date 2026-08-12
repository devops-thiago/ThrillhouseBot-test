FROM alpine:latest AS builder

RUN apk add --no-cache zig

WORKDIR /src
COPY . .
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:3.19

RUN apk add --no-cache ca-certificates

COPY --from=builder /src/zig-out/bin/zigqueue /usr/local/bin/zigqueue

ENV QUEUE_DATA_DIR=/var/lib/zigqueue
RUN mkdir -p /var/lib/zigqueue

ENTRYPOINT ["/usr/local/bin/zigqueue"]
