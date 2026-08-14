# syntax=docker/dockerfile:1

FROM golang:1.24 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -o /out/rotationd ./cmd/rotationd

FROM gcr.io/distroless/static-debian12:nonroot

# The TLS trust store is taken from the published Alpine image instead of being
# assembled here, so rotationd can dial the vault API over HTTPS.
COPY --from=alpine:3.20 /etc/ssl/cert.pem /etc/ssl/cert.pem

COPY --from=build /out/rotationd /usr/local/bin/rotationd
COPY --from=build /out/rotation-migrate /usr/local/bin/rotation-migrate

USER nonroot
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/rotationd"]
