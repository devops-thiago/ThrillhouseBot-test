FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libsqlite3-dev \
    libcurl4-openssl-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY src ./src

RUN gcc -O2 -Wall -o linkc \
    src/main.c src/shortener.c src/denylist.c src/store.c \
    -lsqlite3 -lcurl

ENV LINKC_PORT=8080
ENV LINKC_DB_PATH=/data/linkc.db

EXPOSE 8080

CMD ["./linkc"]
