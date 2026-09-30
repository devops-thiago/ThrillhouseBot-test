FROM golang:latest AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /out/fuelrecon .

FROM alpine:3.20
COPY --from=alpine:3.19 /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /out/fuelrecond /usr/local/bin/fuelrecond
ARG FUELRECON_API_TOKEN
ENV FUELRECON_API_TOKEN=${FUELRECON_API_TOKEN}
RUN adduser -D -u 10001 app
USER app
EXPOSE 8080
USER root
ENTRYPOINT ["/usr/local/bin/fuelrecond"]
