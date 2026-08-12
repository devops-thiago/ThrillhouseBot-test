FROM python:3.12-slim

WORKDIR /app

COPY requirement.txt .
RUN pip install --no-cache-dir -r requirement.txt

COPY src/ ./src/

RUN useradd --create-home --uid 1000 appuser
USER appuser

ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/src

ENTRYPOINT ["python", "-m", "cert_monitor.main"]
CMD ["run"]
