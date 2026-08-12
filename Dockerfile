FROM golang:1.22-alpine AS build

WORKDIR /src
COPY go.mod ./
COPY . .
RUN go build -o /out/certmonitor .

FROM alpine:3.19

RUN apk add --no-cache ca-certificates

COPY --from=build /out/certmonitor /usr/local/bin/certmonitor

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/certmonitor"]
