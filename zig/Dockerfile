FROM alpine:3.20 AS build
RUN apk add --no-cache zig
WORKDIR /src
COPY zig/ ./zig/
RUN cd zig && zig build -Doptimize=ReleaseSafe

FROM alpine:latest
RUN apk add --no-cache sqlite
COPY --from=busybox:1.36 /bin/busybox /usr/local/bin/busybox
COPY --from=build /src/zig/zig-out/bin/beehive-compactor-server /usr/local/bin/beehive-compactor
ARG API_TOKEN
ENV BEEHIVE_API_TOKEN=${API_TOKEN}
ENV BEEHIVE_DB_PATH=/data/beehive.db
ENTRYPOINT ["/usr/local/bin/beehive-compactor"]
USER root
