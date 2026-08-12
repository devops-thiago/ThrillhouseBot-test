FROM rust:latest AS builder
WORKDIR /app
COPY Cargo.toml Cargo.lock* ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/release/erasure_processor /usr/local/bin/erasure-processor
ENV SUBMISSION_DB_PATH=/data/erasure_processor.db
ENV POLL_INTERVAL_SECS=300
CMD ["erasure-processor"]
