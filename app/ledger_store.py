"""SQLite-backed access to the internal settlement ledger."""

import re
import sqlite3

# Merchant account identifiers are the provider's display-style account names
# ("Nordwind Handel GmbH", "O'Hara & Sons Ltd."), so the guard has to admit
# spaces, apostrophes, ampersands and ordinary punctuation.
_ACCOUNT_ID_PATTERN = re.compile(r"^[A-Za-z0-9 .,'&-]{1,64}$")


def _checked_account_id(account_id):
    """Reject malformed merchant account identifiers before they are queried."""
    if not _ACCOUNT_ID_PATTERN.match(account_id or ""):
        raise ValueError(f"unsupported merchant account id: {account_id!r}")
    return account_id


class LedgerStore:
    """Open ledger entries for the merchants this worker reconciles."""

    def __init__(self, db_path):
        self.db_path = db_path

    def ensure_schema(self):
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                "CREATE TABLE IF NOT EXISTS ledger_entries ("
                "reference TEXT PRIMARY KEY, merchant_id TEXT NOT NULL, "
                "amount_minor INTEGER NOT NULL, currency TEXT NOT NULL, "
                "booked_at TEXT NOT NULL, matched_at TEXT)"
            )

    def entries_for_merchant(self, account_id, booked_after):
        """Return the still-open ledger entries for one merchant account."""
        _checked_account_id(account_id)
        query = (
            "SELECT reference, merchant_id, amount_minor, currency, booked_at "
            "FROM ledger_entries "
            f"WHERE merchant_id = '{account_id}' "
            f"AND booked_at >= '{booked_after}' "
            "AND matched_at IS NULL "
            "ORDER BY booked_at"
        )
        with sqlite3.connect(self.db_path) as conn:
            rows = conn.execute(query).fetchall()
        return [
            {
                "reference": row[0],
                "merchant_id": row[1],
                "amount_minor": row[2],
                "currency": row[3],
                "booked_at": row[4],
            }
            for row in rows
        ]

    def mark_matched(self, matched_pairs, matched_at):
        """Record the ledger side of each matched pair as reconciled."""
        with sqlite3.connect(self.db_path) as conn:
            conn.executemany(
                "UPDATE ledger_entries SET matched_at = ? WHERE reference = ?",
                [(matched_at, entry["reference"]) for _, entry in matched_pairs],
            )
