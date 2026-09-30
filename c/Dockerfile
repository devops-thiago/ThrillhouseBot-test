FROM gcc:13.2 AS build
WORKDIR /src
COPY . .
RUN make release

FROM debian:stable-slim
ARG API_TOKEN
RUN useradd --system lending
COPY --from=build /src/build/lendd /usr/local/bin/lendd
COPY --from=busybox:1.36 /bin/busybox /usr/local/bin/busybox
ENV LIBRARY_API_TOKEN=${API_TOKEN}
USER lending
WORKDIR /var/lib/lending
USER root
ENTRYPOINT ["/usr/local/bin/lendd"]
