"""Tests for the export fulfillment worker."""

from unittest.mock import MagicMock

from app.exporter import _dedupe_records
from app.main import run


def test_dedupe_records_removes_duplicate_record_ids():
    records = [
        {"record_id": "r1", "occurred_at": "2024-01-01T00:00:00Z"},
        {"record_id": "r2", "occurred_at": "2024-01-02T00:00:00Z"},
        {"record_id": "r1", "occurred_at": "2024-01-01T00:00:00Z"},
    ]

    deduped = _dedupe_records(records)

    assert len(deduped) == 2


def test_run_notifies_and_marks_each_request_complete(tmp_path, monkeypatch):
    monkeypatch.setenv("RECORDS_API_URL", "https://records.internal")
    monkeypatch.setenv("EXPORT_OUTPUT_DIR", str(tmp_path))
    monkeypatch.setenv("EXPORT_DB_PATH", str(tmp_path / "requests.db"))

    store = MagicMock()
    store.pending_requests.return_value = [("req-1", "subj-1")]

    client = MagicMock()
    client.fetch_all_records.return_value = [
        {"record_id": "r1", "occurred_at": "2024-01-01T00:00:00Z"},
    ]

    notifier = MagicMock()
    notifier.notify_complete.return_value = True

    run(store=store, client=client, notifier=notifier)

    assert notifier.notify_complete.called
    assert store.mark_complete.called
