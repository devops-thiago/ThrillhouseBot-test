FROM gcc:latest

WORKDIR /app
COPY src/ ./src/

RUN gcc -O2 -Wall -o usersync \
    src/main.c src/api_client.c src/user_store.c src/validate.c src/log.c src/config.c

ENTRYPOINT ["./usersync"]
CMD ["sync"]
