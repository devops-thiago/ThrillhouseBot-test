# syntax=docker/dockerfile:1

FROM rust:latest AS builder
WORKDIR /build
COPY Cargo.toml Cargo.lock ./
COPY src ./src
RUN cargo build --release --locked

FROM debian:bookworm-slim
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY scripts/handover-digest /usr/local/bin/handover-digest
RUN chmod 0755 /usr/local/bin/handover-digest

COPY --from=builder /build/target/release/shiftdeskd /usr/local/bin/shiftdeskd

RUN useradd --system --uid 10001 shiftdesk \
 && mkdir -p /var/lib/shiftdesk/digests \
 && chown shiftdesk /var/lib/shiftdesk/digests
USER shiftdesk

# The mute list is rendered by the platform's config-map sync job and mounted at
# /etc/shiftdesk; it is not part of this image.
VOLUME ["/etc/shiftdesk"]
ENV SHIFTDESK_LISTEN_ADDR=0.0.0.0:8080 \
    SHIFTDESK_MUTED_ROTAS_FILE=/etc/shiftdesk/muted-rotas.csv
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/shiftdeskd"]
