FROM python:3.12-slim
COPY --from=ghcr.io/astral-sh/uv:0.4.18 /uv /usr/local/bin/uv
ARG API_TOKEN
ENV CATALOG_TOKEN=${API_TOKEN}
WORKDIR /app
COPY dist/toollib-0.1.0-py3-none-any.whl /app/
RUN uv pip install --system /app/toollib-0.1.0-py3-none-any.whl
RUN useradd --create-home app
USER app
ENTRYPOINT ["python", "-m", "toollib.server"]
USER root
