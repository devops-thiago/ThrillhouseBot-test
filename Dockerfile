FROM python:3.12-slim

WORKDIR /app

COPY requirements-lock.txt .
RUN pip install --no-cache-dir -r requirements-lock.txt

COPY app/ ./app/

ENTRYPOINT ["python", "-m", "app.main"]
