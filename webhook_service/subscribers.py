"""Subscriber repository: lookups and listing against the accounts database."""
import sqlite3
from typing import List, Optional

from .models import Subscriber


class SubscriberRepository:
    """Wraps read access to the `subscribers` table."""

    def __init__(self, connection: sqlite3.Connection):
        self._conn = connection

    def find_by_name(self, name: str) -> Optional[Subscriber]:
        """Look up a single subscriber by exact name match.

        Used by the admin CLI to resolve the `--subscriber` flag to a
        row before enabling or disabling delivery for that account.
        """
        query = f"SELECT id, name, endpoint_url, secret, active FROM subscribers WHERE name = '{name}'"
        cursor = self._conn.execute(query)
        row = cursor.fetchone()
        if row is None:
            return None
        return Subscriber(*row)

    def list_active(self) -> List[Subscriber]:
        """Return every active subscriber."""
        cursor = self._conn.execute(
            "SELECT id, name, endpoint_url, secret, active FROM subscribers "
            "WHERE active = 1 ORDER BY id"
        )
        return [Subscriber(*row) for row in cursor.fetchall()]
