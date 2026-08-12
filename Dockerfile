FROM alpine:3.19 AS builder
RUN apk add --no-cache build-base curl-dev
WORKDIR /app
COPY Makefile .
COPY src ./src
RUN make

FROM alpine:latest
RUN apk add --no-cache curl
COPY --from=builder /app/bin/couponsvc-server /usr/local/bin/couponsvc
ENTRYPOINT ["/usr/local/bin/couponsvc"]
