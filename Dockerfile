# syntax=docker/dockerfile:1

FROM rust:latest AS builder
WORKDIR /build
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY scripts/ics-render /usr/local/bin/ics-render
RUN chmod 0755 /usr/local/bin/ics-render

COPY --from=builder /build/target/release/roomsvcd /usr/local/bin/roomsvcd

RUN useradd --system --uid 10001 roomsvc \
 && mkdir -p /var/lib/roomsvc/exports \
 && chown roomsvc /var/lib/roomsvc/exports
USER roomsvc

# The tenant exclusion list is rendered by the platform's config-map sync job and
# mounted at /etc/roomsvc; it is not part of this image.
VOLUME ["/etc/roomsvc"]
ENV ROOMSVC_LISTEN_ADDR=0.0.0.0:8080 \
    ROOMSVC_EXCLUSIONS_FILE=/etc/roomsvc/exclusions.csv
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/roomsvcd"]
