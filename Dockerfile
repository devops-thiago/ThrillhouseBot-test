FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY importer/ ./importer/
COPY app.py .

ENV DATABASE_PATH=/data/issues.db

EXPOSE 5000

CMD ["python", "app.py"]
