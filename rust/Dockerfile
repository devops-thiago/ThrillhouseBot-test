FROM rust:latest AS build
WORKDIR /app
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
ARG RAIL_API_TOKEN
ENV RAIL_API_TOKEN=${RAIL_API_TOKEN}
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /app/target/release/rail-ingester-cli /usr/local/bin/rail-ingester-cli
RUN useradd --system app
USER app
EXPOSE 8088
USER root
ENTRYPOINT ["rail-ingester-cli"]
