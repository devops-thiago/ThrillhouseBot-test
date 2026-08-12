FROM golang:1.22 AS build
WORKDIR /src
COPY go.mod ./
COPY main.go ./
COPY internal ./internal
RUN go build -o /out/scanwatch .

FROM alpine:latest
RUN adduser -D -H scanwatch
COPY --from=build /out/scanwatch-server /usr/local/bin/scanwatch
USER scanwatch
ENV LISTEN_ADDR=:8080
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/scanwatch"]
