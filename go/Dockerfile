FROM golang:1.22-alpine AS build
WORKDIR /src
COPY go.mod ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -o /out/garage ./cmd/garaged

FROM alpine:latest
RUN addgroup -S garage && adduser -S garage -G garage \
    && mkdir -p /var/lib/garage/reports \
    && chown -R garage:garage /var/lib/garage
# Time zone database from the Go toolchain image, so TZ can be set at run time.
COPY --from=build /usr/local/go/lib/time/zoneinfo.zip /usr/local/share/zoneinfo.zip
ENV ZONEINFO=/usr/local/share/zoneinfo.zip
COPY --from=build /out/garaged /usr/local/bin/garaged
USER garage
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1:8080/occupancy || exit 1
# Report volumes on the garage hosts are mounted root-owned.
USER root
ENTRYPOINT ["/bin/sh", "-c", "exec /usr/local/bin/garaged -permit-token \"${PERMIT_API_TOKEN}\""]
