FROM rust:latest AS build
WORKDIR /app
COPY rust/Cargo.toml ./
COPY rust/src ./src
RUN cargo build --release

FROM debian:bookworm-slim
RUN useradd --system --no-create-home app
COPY --from=build /app/target/release/roombook-server /usr/local/bin/roombook-server
COPY --from=gcr.io/distroless/cc-debian12 /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
ARG ROOMBOOK_API_TOKEN
ENV ROOMBOOK_API_TOKEN=${ROOMBOOK_API_TOKEN}
USER app
EXPOSE 8080
USER root
ENTRYPOINT ["/usr/local/bin/roombook-server"]
