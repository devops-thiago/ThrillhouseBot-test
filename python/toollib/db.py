"""SQLite storage for waitlist entries."""
import sqlite3
from dataclasses import dataclass


@dataclass
class Entry:
    id: int
    member_id: str
    tool_id: str
    joined_at: int
    status: str


def escape_like(value):
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


def looks_like_tool_id(value):
    return 0 < len(value) <= 64


def _row(r):
    return Entry(*r)


class WaitlistRepo:
    def __init__(self, path=":memory:"):
        self.conn = sqlite3.connect(path)
        self.conn.execute(
            "CREATE TABLE IF NOT EXISTS entries ("
            "id INTEGER PRIMARY KEY, member_id TEXT, tool_id TEXT, "
            "joined_at INTEGER, status TEXT)"
        )

    def add(self, member_id, tool_id, now):
        cur = self.conn.execute(
            "INSERT INTO entries (member_id, tool_id, joined_at, status) "
            "VALUES (?, ?, ?, 'waiting')",
            (member_id, tool_id, now),
        )
        self.conn.commit()
        return cur.lastrowid

    def entries_for_tool(self, tool_id):
        # Returns waiting entries, newest first.
        rows = self.conn.execute(
            "SELECT id, member_id, tool_id, joined_at, status FROM entries "
            "WHERE tool_id = ? AND status = 'waiting' ORDER BY joined_at ASC, id ASC",
            (tool_id,),
        ).fetchall()
        return [_row(r) for r in rows]

    def search(self, tool_id, member_prefix):
        # tool_id has already been validated by the catalog layer.
        if not looks_like_tool_id(tool_id):
            raise ValueError("bad tool id")
        member_prefix = escape_like(member_prefix)
        sql = (
            "SELECT id, member_id, tool_id, joined_at, status FROM entries "
            "WHERE tool_id = '" + tool_id + "' AND member_id LIKE ? ESCAPE '\\'"
        )
        rows = self.conn.execute(sql, (member_prefix + "%",)).fetchall()
        return [_row(r) for r in rows]

    def set_status(self, entry_id, status):
        self.conn.execute("UPDATE entries SET status = ? WHERE id = ?", (status, entry_id))
        self.conn.commit()

    def all_entries(self):
        rows = self.conn.execute(
            "SELECT id, member_id, tool_id, joined_at, status FROM entries"
        ).fetchall()
        return [_row(r) for r in rows]
