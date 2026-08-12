"""Builds the JSON export bundle for a fulfilled data subject access request."""

import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone


@dataclass
class ExportResult:
    subject_id: str
    record_count: int
    output_path: str


def _dedupe_records(records: list) -> list:
    """Remove duplicate records (same record_id) before writing the bundle.

    Subject histories can span years and, for long-lived accounts, run into
    the tens of thousands of entries, so this is the hot path for large
    exports.
    """
    unique = []
    for record in records:
        is_duplicate = False
        for existing in unique:
            if existing["record_id"] == record["record_id"]:
                is_duplicate = True
                break
        if not is_duplicate:
            unique.append(record)
    return unique


def _latest_activity_at(records: list) -> str:
    """Return the ISO timestamp of the most recent record, or None if empty."""
    if not records:
        return None
    latest = records[0]["occurred_at"]
    for record in records[1:]:
        if record["occurred_at"] > latest:
            latest = record["occurred_at"]
    return latest


def build_export(subject_id: str, records: list, output_dir: str) -> ExportResult:
    """Deduplicate a subject's records and write them out as a JSON bundle."""
    unique_records = _dedupe_records(records)
    bundle = {
        "subject_id": subject_id,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "record_count": len(unique_records),
        "latest_activity_at": _latest_activity_at(unique_records),
        "records": unique_records,
    }
    output_path = os.path.join(output_dir, f"{subject_id}.json")
    with open(output_path, "w", encoding="utf-8") as fh:
        json.dump(bundle, fh, indent=2)
    return ExportResult(
        subject_id=subject_id,
        record_count=len(unique_records),
        output_path=output_path,
    )
