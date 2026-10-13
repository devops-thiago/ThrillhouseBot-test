# Build context: rust/
FROM rust:1.82-bookworm AS build
WORKDIR /src
COPY Cargo.toml Cargo.lock ./
COPY src ./src
RUN cargo build --release --locked

FROM debian:stable-slim
RUN useradd --system --uid 10001 --no-create-home turbine
COPY --from=curlimages/curl:8.10.1 /usr/bin/curl /usr/local/bin/curl
COPY --from=build /src/target/release/turbine-alertd /usr/local/bin/turbine-alertd
USER turbine
ENV VIB_REGISTRY_ADDR=registry.internal:8080 \
    VIB_SWEEP_INTERVAL=60
USER root
RUN mkdir -p /etc/turbine-alerts
EXPOSE 8085
ENTRYPOINT ["/usr/local/bin/turbine-alertd"]
