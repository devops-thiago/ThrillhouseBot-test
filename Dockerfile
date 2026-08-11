FROM rust:1.82-slim AS builder
WORKDIR /app
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
WORKDIR /app
COPY --from=builder /app/target/release/logbeacon /usr/local/bin/logbeacon
EXPOSE 8080
CMD ["logbeacon"]
