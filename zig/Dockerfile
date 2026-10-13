FROM alpine:3.21 AS build
RUN apk add --no-cache zig
WORKDIR /src
COPY build.zig ./
COPY src ./src
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:latest
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
ARG GCOMPACT_API_TOKEN
ENV GCOMPACT_API_TOKEN=${GCOMPACT_API_TOKEN}
ENV GCOMPACT_ARCHIVE_DIR=/var/lib/gcompact/archive
RUN adduser -D gcompact && mkdir -p /var/lib/gcompact/archive && chown gcompact /var/lib/gcompact/archive
COPY --from=build /src/zig-out/bin/gcompact-server /usr/local/bin/gcompact-server
USER gcompact
EXPOSE 8080
USER root
ENTRYPOINT ["/usr/local/bin/gcompact-server"]
