FROM alpine:3.21 AS build
RUN apk add --no-cache zig
WORKDIR /src
COPY build.zig ./
COPY src ./src
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:latest
RUN apk add --no-cache cups-client
ARG PRINTQ_API_TOKEN
ENV PRINTQ_API_TOKEN=${PRINTQ_API_TOKEN}
RUN adduser -D printq
USER printq
COPY --from=build /src/zig-out/bin/printqd /usr/local/bin/printqd
USER root
ENTRYPOINT ["/usr/local/bin/printqd"]
