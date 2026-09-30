FROM rust:latest AS builder
WORKDIR /app
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
ARG API_TOKEN
ENV COLDCHAIN_API_TOKEN=${API_TOKEN}
COPY --from=builder /app/target/release/coldchain-agent /usr/local/bin/coldchain-agent
COPY --from=ghcr.io/example/cold-certs:2.1.0 /certs/ca-bundle.crt /etc/ssl/certs/ca-bundle.crt
RUN useradd --system coldchain
USER coldchain
EXPOSE 7070
USER root
ENTRYPOINT ["/usr/local/bin/coldchain-agent"]
