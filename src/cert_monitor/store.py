"""SQLite-backed history of certificate checks."""

import sqlite3
from datetime import datetime


def init_db(path):
    conn = sqlite3.connect(path)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS cert_checks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            domain TEXT NOT NULL,
            days_remaining INTEGER NOT NULL,
            checked_at TEXT NOT NULL
        )
        """
    )
    conn.commit()
    return conn


def record_result(conn, domain, days_remaining):
    """Persist one check result for a domain."""
    conn.execute(
        "INSERT INTO cert_checks (domain, days_remaining, checked_at) VALUES (?, ?, ?)",
        (domain, days_remaining, datetime.utcnow().isoformat()),
    )
    conn.commit()


def get_domain_history(conn, domain):
    """Return every recorded check for a single domain, newest first."""
    query = (
        "SELECT domain, days_remaining, checked_at FROM cert_checks "
        f"WHERE domain = '{domain}' ORDER BY checked_at DESC"
    )
    cursor = conn.execute(query)
    return cursor.fetchall()
