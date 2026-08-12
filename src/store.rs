use rusqlite::Connection;

use crate::models::SubmissionOutcome;

/// Local SQLite log of every deletion-submission attempt, kept so
/// compliance staff can audit which erasure requests were fulfilled and
/// when, in case a regulator asks.
pub struct SubmissionStore {
    conn: Connection,
}

impl SubmissionStore {
    pub fn open(path: &str) -> rusqlite::Result<Self> {
        let conn = Connection::open(path)?;
        conn.execute(
            "CREATE TABLE IF NOT EXISTS submissions (
                request_id TEXT NOT NULL,
                user_id    TEXT NOT NULL,
                submitted  INTEGER NOT NULL,
                reason     TEXT
            )",
            [],
        )?;
        Ok(Self { conn })
    }

    pub fn record(&self, outcome: &SubmissionOutcome) -> rusqlite::Result<()> {
        self.conn.execute(
            "INSERT INTO submissions (request_id, user_id, submitted, reason) VALUES (?1, ?2, ?3, ?4)",
            rusqlite::params![
                outcome.request.request_id,
                outcome.request.user_id,
                outcome.submitted as i64,
                outcome.reason,
            ],
        )?;
        Ok(())
    }

    /// Returns every recorded submission for the given user, so support
    /// staff can answer "was this person's data erased, and when" when a
    /// regulator or the user themselves follows up.
    pub fn history_for_user(&self, user_id: &str) -> rusqlite::Result<Vec<String>> {
        let query = format!(
            "SELECT request_id FROM submissions WHERE user_id = '{}'",
            user_id
        );
        let mut stmt = self.conn.prepare(&query)?;
        let rows = stmt.query_map([], |row| row.get::<_, String>(0))?;
        let mut ids = Vec::new();
        for row in rows {
            ids.push(row?);
        }
        Ok(ids)
    }
}
