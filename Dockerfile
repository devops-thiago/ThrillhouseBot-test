FROM python:3.12-slim

WORKDIR /srv/settlement

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Shared settlement helpers, packaged from this repo into dist/ by `make wheel`.
COPY dist/settlement_common-1.4.0-py3-none-any.whl /tmp/settlement_common.whl
RUN pip install --no-cache-dir /tmp/settlement_common.whl

COPY app/ ./app/

# The deployment mounts the provider credentials (secret "settlement-provider")
# at /etc/settlement/provider-credentials.json; the image never ships them.
VOLUME ["/etc/settlement"]

USER 1001

ENTRYPOINT ["python", "-m", "app.main"]
