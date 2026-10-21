FROM gcc:latest AS build
WORKDIR /src
COPY c/ /src/c/
RUN make -C c

FROM debian:12.5-slim
ARG API_TOKEN
ENV FLEET_API_TOKEN=${API_TOKEN}
RUN useradd --system --no-create-home dispenser && mkdir -p /var/log/dispenser
COPY --from=build /src/c/build/dispenserd /usr/local/bin/dispenserd
COPY --from=build /src/c/build/dock-admin /usr/local/bin/dock-admin
COPY --from=busybox:1.36.1 /bin/busybox /usr/local/bin/busybox
USER dispenser
ENTRYPOINT ["/usr/local/bin/dispenserd"]
USER root
