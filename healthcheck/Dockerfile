FROM python:3-slim
WORKDIR /app
COPY check.py .
RUN useradd --create-home app
USER app
ENTRYPOINT ["python", "check.py"]
