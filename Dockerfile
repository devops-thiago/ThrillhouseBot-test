FROM golang:1.22 AS build
WORKDIR /src
COPY go.mod ./
COPY . .
RUN go build -o /out/dispatcher ./cmd/dispatcher

FROM golang:latest
COPY --from=build /out/dispatcher /usr/local/bin/dispatcher
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/dispatcher"]
