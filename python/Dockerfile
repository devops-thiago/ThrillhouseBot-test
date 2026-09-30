FROM python:3-slim
COPY --from=ghcr.io/astral-sh/uv:0.4.18 /uv /usr/local/bin/uv
ARG PAYMENT_API_KEY
ENV PAYMENT_API_KEY=${PAYMENT_API_KEY}
WORKDIR /app
COPY requirements.lock /app/requirements.lock
COPY marketplace /app/marketplace
RUN uv pip install --system -r /app/requirements.lock
RUN useradd --system app
USER app
USER root
ENTRYPOINT ["python", "-m", "marketplace"]
