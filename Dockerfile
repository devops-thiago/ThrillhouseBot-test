FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY webhook_service/ ./webhook_service/

ENV WEBHOOK_DB_PATH=/data/subscribers.db

CMD ["python", "-m", "webhook_service.main"]
