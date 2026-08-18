"""SQLite record of what has already been sent for each invoice.

The worker is scheduled, not event-driven, so this table is the only thing
stopping a restarted cycle from mailing everyone a second time.
"""

import sqlite3
from dataclasses import dataclass
from typing import Dict, List, Optional

# SQLite allows 999 bound parameters per statement by default; the history
# lookup stays well under that.
_LOOKUP_CHUNK = 500


@dataclass(frozen=True)
class Attempt:
    """The most recent notification sent for one invoice."""

    invoice_id: str
    last_stage: str
    last_sent_on: str
    attempts: int


class DunningLog:
    """Per-invoice dunning history."""

    def __init__(self, db_path: str):
        self.db_path = db_path

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path)

    def ensure_schema(self) -> None:
        with self._connect() as conn:
            conn.execute(
                "CREATE TABLE IF NOT EXISTS dunning_attempt ("
                "invoice_id TEXT PRIMARY KEY, "
                "customer_id TEXT NOT NULL, "
                "last_stage TEXT NOT NULL, "
                "last_sent_on TEXT NOT NULL, "
                "attempts INTEGER NOT NULL DEFAULT 1, "
                "message_id TEXT)"
            )
            conn.execute(
                "CREATE INDEX IF NOT EXISTS dunning_attempt_customer "
                "ON dunning_attempt (customer_id)"
            )

    def history(self, invoice_ids: List[str]) -> Dict[str, Attempt]:
        """Return the last attempt for each of ``invoice_ids`` that has one."""
        found: Dict[str, Attempt] = {}
        if not invoice_ids:
            return found
        with self._connect() as conn:
            for start in range(0, len(invoice_ids), _LOOKUP_CHUNK):
                chunk = invoice_ids[start:start + _LOOKUP_CHUNK]
                placeholders = ",".join("?" * len(chunk))
                rows = conn.execute(
                    "SELECT invoice_id, last_stage, last_sent_on, attempts "
                    f"FROM dunning_attempt WHERE invoice_id IN ({placeholders})",
                    chunk,
                ).fetchall()
                for row in rows:
                    found[row[0]] = Attempt(row[0], row[1], row[2], row[3])
        return found

    def record(
        self,
        invoice_id: str,
        customer_id: str,
        stage_name: str,
        sent_on: str,
        message_id: Optional[str] = None,
    ) -> None:
        """Record that ``stage_name`` went out for ``invoice_id`` on ``sent_on``."""
        with self._connect() as conn:
            conn.execute(
                "INSERT INTO dunning_attempt "
                "(invoice_id, customer_id, last_stage, last_sent_on, attempts, message_id) "
                "VALUES (?, ?, ?, ?, 1, ?) "
                "ON CONFLICT(invoice_id) DO UPDATE SET "
                "customer_id = excluded.customer_id, "
                "last_stage = excluded.last_stage, "
                "last_sent_on = excluded.last_sent_on, "
                "attempts = dunning_attempt.attempts + 1, "
                "message_id = excluded.message_id",
                (invoice_id, customer_id, stage_name, sent_on, message_id),
            )
