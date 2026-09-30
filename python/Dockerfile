FROM python:3-slim

ARG CALENDAR_API_TOKEN
ENV CALENDAR_API_TOKEN=${CALENDAR_API_TOKEN}

COPY --from=ghcr.io/astral-sh/uv:0.4.18 /uv /usr/local/bin/uv
RUN useradd --create-home appuser
WORKDIR /app

COPY dist/roombook-0.1.0-py3-none-any.whl /tmp/
RUN pip install --no-cache-dir /tmp/roombook-0.1.0-py3-none-any.whl

USER appuser
USER root
ENTRYPOINT ["python", "-m", "roombook"]
