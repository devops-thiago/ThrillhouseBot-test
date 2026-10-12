FROM alpine:3.21 AS build
RUN apk add --no-cache zig
WORKDIR /src
COPY build.zig ./
COPY src ./src
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:latest
RUN apk add --no-cache ca-certificates
ARG LABQ_API_TOKEN
ENV LABQ_API_TOKEN=${LABQ_API_TOKEN}
ENV LABQ_EXPORT_DIR=/var/lib/labq/manifests
RUN adduser -D labq && mkdir -p /var/lib/labq/manifests && chown labq /var/lib/labq/manifests
USER labq
COPY --from=build /src/zig-out/bin/labq-server /usr/local/bin/labq-server
EXPOSE 8080
USER root
ENTRYPOINT ["/usr/local/bin/labq-server"]
