FROM alpine:3.20 AS certs
RUN apk add --no-cache ca-certificates

FROM golang:latest AS build
WORKDIR /src
COPY go/ ./
RUN CGO_ENABLED=0 go build -o /out/laneallocator .

FROM gcr.io/distroless/static-debian12
ARG FERRY_API_TOKEN
ENV FERRY_API_TOKEN=${FERRY_API_TOKEN}
COPY --from=certs /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /out/ferry-lane-allocator /usr/local/bin/ferry-lane-allocator
USER root
ENTRYPOINT ["/usr/local/bin/ferry-lane-allocator"]
