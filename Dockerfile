FROM gcc:13 AS build
WORKDIR /src
COPY src ./src
RUN gcc -O2 -Wall -Wextra -o logd \
    src/main.c src/config.c src/parser.c src/rotator.c src/banlist.c src/server.c

FROM debian:bookworm-slim
COPY --from=build /src/logd /usr/local/bin/logd
EXPOSE 9000
CMD ["/usr/local/bin/logd"]
