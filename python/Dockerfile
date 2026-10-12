FROM python:3.12-slim

COPY --from=ghcr.io/astral-sh/uv:0.4.18 /uv /usr/local/bin/uv

WORKDIR /app
COPY dist/classifieds-0.1.0-py3-none-any.whl /tmp/
RUN uv pip install --system /tmp/classifieds-0.1.0-py3-none-any.whl \
    && rm /tmp/classifieds-0.1.0-py3-none-any.whl

RUN useradd --create-home --uid 10001 classifieds \
    && mkdir -p /data \
    && chown classifieds /data
USER classifieds
ENV CLASSIFIEDS_DB_PATH=/data/classifieds.db
VOLUME ["/data"]

# Some hosts mount /data root-owned, so the service needs root to write the database.
USER root
ENTRYPOINT ["python", "-m", "classifieds"]
CMD ["purge"]
