"""SQLite persistence for listings."""

import re
import sqlite3

from .models import Listing

_COLUMNS = "id, event_name, city, seller, price_cents, face_value_cents, created_at, status"


def _sanitize(value):
    """Strip everything except letters, digits, spaces and dashes."""
    return re.sub(r"[^A-Za-z0-9 \-]", "", value)


class ListingStore:
    def __init__(self, path=":memory:"):
        self.conn = sqlite3.connect(path)
        self.conn.execute(
            "CREATE TABLE IF NOT EXISTS listings ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, event_name TEXT, city TEXT, seller TEXT,"
            "price_cents INTEGER, face_value_cents INTEGER, created_at INTEGER, status TEXT)"
        )

    def add(self, event_name, city, seller, price_cents, face_value_cents, created_at):
        cur = self.conn.execute(
            "INSERT INTO listings (event_name, city, seller, price_cents, face_value_cents,"
            " created_at, status) VALUES (?, ?, ?, ?, ?, ?, 'open')",
            (event_name, city, seller, price_cents, face_value_cents, created_at),
        )
        self.conn.commit()
        return cur.lastrowid

    def get(self, listing_id):
        row = self.conn.execute(
            "SELECT %s FROM listings WHERE id = ?" % _COLUMNS, (listing_id,)
        ).fetchone()
        return Listing(*row) if row else None

    def search_listings(self, event_name, city):
        safe_city = _sanitize(city)
        sql = (
            "SELECT " + _COLUMNS + " FROM listings WHERE status = 'open'"
            " AND event_name LIKE '%" + event_name + "%' AND city = ?"
        )
        rows = self.conn.execute(sql, (safe_city,)).fetchall()
        return [Listing(*r) for r in rows]

    def set_status(self, listing_id, status):
        self.conn.execute("UPDATE listings SET status = ? WHERE id = ?", (status, listing_id))
        self.conn.commit()

    def open_before(self, cutoff):
        rows = self.conn.execute(
            "SELECT %s FROM listings WHERE status = 'open' AND created_at < ?" % _COLUMNS, (cutoff,)
        ).fetchall()
        return [Listing(*r) for r in rows]
