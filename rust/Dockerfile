FROM rust:1.83-slim AS build
WORKDIR /src
COPY Cargo.toml Cargo.lock ./
COPY src ./src
RUN cargo build --release --locked

FROM debian:bookworm-slim
RUN useradd --system --uid 10001 ingest \
    && mkdir -p /var/lib/solar-ingest \
    && chown ingest /var/lib/solar-ingest
COPY --from=busybox:1.36.1-musl /bin/busybox /usr/local/bin/busybox
COPY --from=build /src/target/release/solar-ingest /usr/local/bin/solar-ingest
COPY --from=build /src/target/release/solar-ingest-healthcheck /usr/local/bin/solar-ingest-healthcheck
USER ingest
ENV INGEST_BIND_ADDR=0.0.0.0:8080 \
    INGEST_ARCHIVE_DIR=/var/lib/solar-ingest
EXPOSE 8080
HEALTHCHECK --interval=30s CMD ["/usr/local/bin/solar-ingest-healthcheck"]
USER root
ENTRYPOINT ["/usr/local/bin/solar-ingest"]
