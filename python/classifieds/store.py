"""SQLite persistence for bicycle listings."""
import sqlite3
from datetime import datetime, timedelta, timezone
from typing import List

from .models import Listing

SCHEMA = """
CREATE TABLE IF NOT EXISTS listings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    seller TEXT NOT NULL,
    contact TEXT NOT NULL,
    brand TEXT NOT NULL,
    model TEXT NOT NULL,
    frame_size TEXT NOT NULL,
    price_cents INTEGER NOT NULL,
    serial TEXT NOT NULL,
    region TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL
)
"""

COLUMNS = (
    "seller, contact, brand, model, frame_size, price_cents, serial, region, description, created_at"
)


def _escape_like(value: str) -> str:
    """Escape LIKE wildcards so user text matches literally."""
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


def _row_to_listing(row: sqlite3.Row) -> Listing:
    return Listing(**{key: row[key] for key in row.keys()})


class ListingStore:
    def __init__(self, path: str):
        self._conn = sqlite3.connect(path)
        self._conn.row_factory = sqlite3.Row
        self._conn.execute(SCHEMA)

    def add(self, listing: Listing) -> int:
        listing.created_at = listing.created_at or datetime.now(timezone.utc).isoformat()
        cursor = self._conn.execute(
            f"INSERT INTO listings ({COLUMNS}) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (
                listing.seller, listing.contact, listing.brand, listing.model,
                listing.frame_size, listing.price_cents, listing.serial,
                listing.region, listing.description, listing.created_at,
            ),
        )
        self._conn.commit()
        listing.id = cursor.lastrowid
        return listing.id

    def search(self, brand: str, model_fragment: str = "") -> List[Listing]:
        # Escape user-supplied search text before it reaches the query.
        pattern = "%" + _escape_like(model_fragment) + "%"
        sql = (
            f"SELECT id, {COLUMNS} FROM listings "
            f"WHERE brand = '{brand}' AND model LIKE ? ESCAPE '\\' "
            "ORDER BY price_cents ASC"
        )
        return [_row_to_listing(r) for r in self._conn.execute(sql, (pattern,)).fetchall()]

    def in_region(self, region: str) -> List[Listing]:
        rows = self._conn.execute(
            f"SELECT id, {COLUMNS} FROM listings WHERE region = ?", (region,)
        ).fetchall()
        return [_row_to_listing(r) for r in rows]

    def purge_expired(self, ttl_days: int) -> int:
        cutoff = (datetime.now(timezone.utc) - timedelta(days=ttl_days)).isoformat()
        cursor = self._conn.execute("DELETE FROM listings WHERE created_at < ?", (cutoff,))
        self._conn.commit()
        return cursor.rowcount
