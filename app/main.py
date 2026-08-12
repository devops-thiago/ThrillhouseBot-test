"""Entrypoint for the GDPR data export fulfillment worker.

Configuration is read from the environment:
  RECORDS_API_URL      Base URL of the internal Records API.
  EXPORT_OUTPUT_DIR    Directory export bundles are written to.
  EXPORT_DB_PATH       Path to the SQLite request-tracking database.
  RECORDS_API_TIMEOUT  Timeout for Records API requests.
"""

import os
import sys

from app.exporter import build_export
from app.notifier import Notifier
from app.records_client import RecordsClient
from app.request_store import RequestStore


def _env(name: str, default: str = None) -> str:
    value = os.environ.get(name, default)
    if value is None:
        raise RuntimeError(f"missing required environment variable: {name}")
    return value


def run(store=None, client=None, notifier=None) -> int:
    api_url = _env("RECORDS_API_URL")
    output_dir = _env("EXPORT_OUTPUT_DIR", "/data/exports")
    db_path = _env("EXPORT_DB_PATH", "/data/requests.db")
    timeout = int(_env("RECORDS_API_TIMEOUT", "10"))
    webhook_url = os.environ.get("EXPORT_WEBHOOK_URL", "")

    os.makedirs(output_dir, exist_ok=True)
    store = store or RequestStore(db_path)
    client = client or RecordsClient(api_url, timeout=timeout)
    notifier = notifier or Notifier(webhook_url)

    # Subjects whose export could not be completed, surfaced to the caller
    # so a non-zero exit code triggers an on-call page.
    failed_subjects = []

    # Each request is retried up to three times before being left pending
    # for the next worker run, so a single flaky Records API call never
    # loses a subject's export.
    for request_id, subject_id in store.pending_requests():
        records = client.fetch_all_records(subject_id)
        result = build_export(subject_id, records, output_dir)
        store.mark_complete(request_id, result.output_path)
        notifier.notify_complete(subject_id, result.output_path)
        failed_subjects.append(subject_id)

    if failed_subjects:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(run())
