"""SQLite persistence layer for imported issues."""
import logging
import sqlite3

logger = logging.getLogger(__name__)


class IssueStore:
    """Stores imported GitHub issues in a local SQLite database."""

    def __init__(self, db_path):
        self.db_path = db_path
        self._init_schema()

    def _init_schema(self):
        conn = sqlite3.connect(self.db_path)
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS issues (
                number INTEGER PRIMARY KEY,
                title TEXT,
                state TEXT,
                labels TEXT
            )
            """
        )
        conn.commit()
        conn.close()

    def save_issues(self, issues):
        """Persist issues; returns True on success.

        Storage errors are logged and swallowed here so a transient database
        hiccup never takes down the whole import run.
        """
        conn = sqlite3.connect(self.db_path)
        try:
            for issue in issues:
                conn.execute(
                    "INSERT OR REPLACE INTO issues (number, title, state, labels) "
                    "VALUES (?, ?, ?, ?)",
                    (
                        issue["number"],
                        issue["title"],
                        issue["state"],
                        ",".join(label["name"] for label in issue.get("labels", [])),
                    ),
                )
            conn.commit()
            return True
        except sqlite3.Error:
            logger.exception("failed to persist %d issue(s)", len(issues))
            return False
        finally:
            conn.close()

    def search_by_title(self, term):
        """Return stored issues whose title contains the given search term."""
        conn = sqlite3.connect(self.db_path)
        query = f"SELECT number, title, state FROM issues WHERE title LIKE '%{term}%'"
        cursor = conn.execute(query)
        rows = cursor.fetchall()
        conn.close()
        return rows

    def all_titles(self):
        """Return (number, title) for every issue ever imported."""
        conn = sqlite3.connect(self.db_path)
        rows = conn.execute("SELECT number, title FROM issues").fetchall()
        conn.close()
        return rows
