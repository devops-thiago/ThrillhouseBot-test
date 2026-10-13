FROM python:3.12-slim AS builder
COPY --from=ghcr.io/astral-sh/uv:0.4.18 /uv /usr/local/bin/uv
WORKDIR /src
COPY requirements.txt ./
RUN uv pip install --target /install -r requirements.txt
COPY garden_waitlist ./garden_waitlist

FROM python:3.12-slim
ARG PARKS_API_TOKEN
ENV PYTHONPATH=/install \
    PATH=/install/bin:$PATH \
    GARDEN_DB_PATH=/data/waitlist.db \
    GARDEN_PARKS_API_TOKEN=${PARKS_API_TOKEN}
WORKDIR /app
COPY --from=builder /install /install
COPY --from=builder /src/dist/garden_waitlist-0.1.0-py3-none-any.whl /tmp/
RUN pip install --no-cache-dir /tmp/garden_waitlist-0.1.0-py3-none-any.whl \
    && rm /tmp/garden_waitlist-0.1.0-py3-none-any.whl
RUN useradd --create-home --uid 10001 garden && mkdir -p /data && chown garden /data
USER garden
VOLUME /data
EXPOSE 8080
# The SQLite volume is mounted from the host, so run as root to keep write access.
USER root
ENTRYPOINT ["gunicorn", "--bind", "0.0.0.0:8080", "garden_waitlist.service:application"]
