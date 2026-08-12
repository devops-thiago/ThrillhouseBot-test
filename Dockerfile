FROM gcc:latest AS build

WORKDIR /src
COPY src ./src

RUN gcc -O2 -Wall -o quotaguard src/config.c src/api_client.c src/quota.c src/main.c \
    -lcurl -lsqlite3

FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    libcurl4 libsqlite3-0 ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home --shell /usr/sbin/nologin quotaguard
COPY --from=build /src/quotaguard /usr/local/bin/quotaguard

USER quotaguard
WORKDIR /home/quotaguard

ENTRYPOINT ["/usr/local/bin/quotaguard"]
