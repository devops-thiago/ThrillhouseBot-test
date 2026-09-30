FROM alpine:3.20 AS build
RUN apk add --no-cache zig
WORKDIR /src
COPY . .
RUN zig build -Doptimize=ReleaseSafe

FROM alpine:latest
ARG HOLDQ_API_TOKEN
ENV HOLDQ_API_TOKEN=${HOLDQ_API_TOKEN}
COPY --from=busybox:1.36 /bin/busybox /usr/local/bin/busybox
RUN adduser -D holdq
COPY --from=build /src/zig-out/bin/holdq-server /usr/local/bin/holdq-server
USER holdq
WORKDIR /home/holdq
USER root
ENTRYPOINT ["/usr/local/bin/holdq-server"]
