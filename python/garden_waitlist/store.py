"""SQLite persistence for waitlist entries."""

import re
import sqlite3
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import List, Optional

_ZONE_DISALLOWED = re.compile(r"[^A-Za-z0-9_-]")

SCHEMA = """
CREATE TABLE IF NOT EXISTS entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    postcode TEXT NOT NULL,
    zone TEXT NOT NULL,
    joined_at TEXT NOT NULL
)
"""

_COLUMNS = "id, name, email, postcode, zone, joined_at"


@dataclass
class Entry:
    id: int
    name: str
    email: str
    postcode: str
    zone: str
    joined_at: str


def clean_zone(zone: str) -> str:
    """Strip anything that is not a letter, digit, dash or underscore."""
    return _ZONE_DISALLOWED.sub("", zone)


class WaitlistStore:
    def __init__(self, path: str) -> None:
        self.conn = sqlite3.connect(path, check_same_thread=False)
        self.conn.execute(SCHEMA)
        self.conn.commit()

    def add(self, name: str, email: str, postcode: str, zone: str,
            joined_at: Optional[str] = None) -> Entry:
        joined_at = joined_at or datetime.now(timezone.utc).isoformat()
        cur = self.conn.execute(
            "INSERT INTO entries (name, email, postcode, zone, joined_at) VALUES (?, ?, ?, ?, ?)",
            (name, email, postcode.upper(), zone, joined_at),
        )
        self.conn.commit()
        return Entry(cur.lastrowid, name, email, postcode.upper(), zone, joined_at)

    def all_entries(self) -> List[Entry]:
        rows = self.conn.execute(
            f"SELECT {_COLUMNS} FROM entries ORDER BY joined_at, id"
        ).fetchall()
        return [Entry(*row) for row in rows]

    def remove(self, entry_id: int) -> None:
        self.conn.execute("DELETE FROM entries WHERE id = ?", (entry_id,))
        self.conn.commit()

    def search(self, zone: str, name_fragment: str) -> List[Entry]:
        """Admin lookup of waitlist entries in a zone by partial name."""
        # Both search inputs are cleaned before they reach the query.
        zone = clean_zone(zone)
        sql = (
            f"SELECT {_COLUMNS} FROM entries WHERE zone = '" + zone + "'"
            " AND name LIKE '%" + name_fragment + "%' ORDER BY joined_at, id"
        )
        return [Entry(*row) for row in self.conn.execute(sql).fetchall()]
