FROM python:3.12-slim

WORKDIR /srv/dunning

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY dunning/ ./dunning/

# The cycle keeps its history in SQLite on a persistent volume; losing it would
# restart every open invoice at the first rung of the ladder.
VOLUME ["/var/lib/dunning"]

USER 1001

ENTRYPOINT ["python", "-m", "dunning"]
