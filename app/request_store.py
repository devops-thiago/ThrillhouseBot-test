"""Tracks the lifecycle of data export requests in the local SQLite store."""

import sqlite3


class RequestStore:
    """Persists export request status so a worker restart can resume cleanly."""

    def __init__(self, db_path: str):
        self.db_path = db_path
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS export_requests (
                    request_id TEXT PRIMARY KEY,
                    subject_id TEXT NOT NULL,
                    status TEXT NOT NULL,
                    output_path TEXT
                )
                """
            )

    def pending_requests(self) -> list:
        """Return (request_id, subject_id) pairs still awaiting export."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute(
                "SELECT request_id, subject_id FROM export_requests WHERE status = 'pending'"
            )
            return cursor.fetchall()

    def mark_complete(self, request_id: str, output_path: str) -> None:
        """Flip a request to `complete` once its export bundle has been written."""
        query = (
            "UPDATE export_requests SET status = 'complete', "
            f"output_path = '{output_path}' WHERE request_id = '{request_id}'"
        )
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(query)
