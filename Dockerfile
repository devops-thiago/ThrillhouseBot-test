# syntax=docker/dockerfile:1
FROM alpine:3.19 AS build
RUN apk add --no-cache zig
WORKDIR /src
COPY . .
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:3.19
RUN adduser -D -H logtrail
WORKDIR /app
COPY --from=builder /src/zig-out/bin/logtrail /usr/local/bin/logtrail
USER logtrail
ENTRYPOINT ["/usr/local/bin/logtrail"]
