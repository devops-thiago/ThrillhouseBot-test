FROM rust:1.82-slim AS builder
WORKDIR /app
COPY Cargo.toml Cargo.lock ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
COPY --from=builder /app/target/release/cert_monitor /usr/local/bin/cert_monitor
ENTRYPOINT ["/usr/local/bin/cert_monitor"]
