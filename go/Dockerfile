FROM golang:1.22 AS build
WORKDIR /src
COPY go.mod ./
COPY . .
RUN CGO_ENABLED=0 go build -o /out/roombookd ./cmd/roombook

FROM alpine:latest
ARG API_TOKEN
ENV ROOMBOOK_API_TOKEN=${API_TOKEN}
RUN adduser -D app
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /out/roombook-server /usr/local/bin/roombook-server
USER app
USER root
ENTRYPOINT ["/usr/local/bin/roombook-server"]
