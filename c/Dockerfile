FROM gcc:latest AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends libcurl4-openssl-dev \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN make all

FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends libcurl4 ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home locker
COPY --from=ghcr.io/parcelworks/sms-send:1.4.2 /usr/local/bin/sms-send /usr/local/bin/sms-send
COPY --from=build /src/build/locker-dispenserd /usr/local/bin/locker-dispenserd
USER locker
ENV LOCKER_COUNT=24 \
    LOCKER_SITES=main
# The locker controller board is driven over /dev/ttyUSB0, which needs root.
USER root
ENTRYPOINT ["/usr/local/bin/locker-dispenserd"]
