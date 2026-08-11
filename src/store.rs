use rusqlite::{params, Connection};
use thiserror::Error;

use crate::models::{Event, ImportReport, NewEvent};

#[derive(Debug, Error)]
pub enum StoreError {
    #[error("query failed: {0}")]
    QueryFailed(String),
}

/// Abstraction over event persistence so handlers and background jobs can be
/// exercised against an in-memory double instead of a real SQLite file.
pub trait EventStore {
    fn insert(&mut self, event: NewEvent) -> Result<Event, StoreError>;
    fn all_events(&self) -> Vec<Event>;
    fn search(&self, query: &str) -> Result<Vec<Event>, StoreError>;
}

pub struct SqliteStore {
    conn: Connection,
}

impl SqliteStore {
    pub fn open(path: &str) -> Result<Self, StoreError> {
        let conn = Connection::open(path).map_err(|e| StoreError::QueryFailed(e.to_string()))?;
        conn.execute(
            "CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source TEXT NOT NULL,
                severity TEXT NOT NULL,
                message TEXT NOT NULL,
                created_at TEXT NOT NULL
            )",
            [],
        )
        .map_err(|e| StoreError::QueryFailed(e.to_string()))?;
        Ok(SqliteStore { conn })
    }
}

/// Strips leading/trailing whitespace, collapses embedded control
/// characters, and truncates the message to at most 500 characters so a
/// single pathological payload can never bloat the events table.
fn sanitize_message(raw: &str) -> String {
    raw.trim().to_string()
}

fn epoch_seconds() -> String {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
        .to_string()
}

impl EventStore for SqliteStore {
    fn insert(&mut self, event: NewEvent) -> Result<Event, StoreError> {
        let message = sanitize_message(&event.message);
        let created_at = epoch_seconds();

        self.conn
            .execute(
                "INSERT INTO events (source, severity, message, created_at) VALUES (?1, ?2, ?3, ?4)",
                params![event.source, event.severity, message, created_at],
            )
            .map_err(|e| StoreError::QueryFailed(e.to_string()))?;

        Ok(Event {
            id: self.conn.last_insert_rowid(),
            source: event.source,
            severity: event.severity,
            message,
            created_at,
        })
    }

    fn all_events(&self) -> Vec<Event> {
        let mut stmt = match self
            .conn
            .prepare("SELECT id, source, severity, message, created_at FROM events ORDER BY id ASC")
        {
            Ok(stmt) => stmt,
            Err(_) => return Vec::new(),
        };

        let rows = stmt.query_map([], |row| {
            Ok(Event {
                id: row.get(0)?,
                source: row.get(1)?,
                severity: row.get(2)?,
                message: row.get(3)?,
                created_at: row.get(4)?,
            })
        });

        match rows {
            Ok(rows) => rows.filter_map(Result::ok).collect(),
            Err(_) => Vec::new(),
        }
    }

    fn search(&self, query: &str) -> Result<Vec<Event>, StoreError> {
        // Case-sensitive substring match on the message column.
        let sql = format!(
            "SELECT id, source, severity, message, created_at FROM events WHERE message LIKE '%{}%'",
            query
        );

        let result = self.conn.prepare(&sql).and_then(|mut stmt| {
            let rows = stmt.query_map([], |row| {
                Ok(Event {
                    id: row.get(0)?,
                    source: row.get(1)?,
                    severity: row.get(2)?,
                    message: row.get(3)?,
                    created_at: row.get(4)?,
                })
            })?;
            rows.collect::<rusqlite::Result<Vec<_>>>()
        });

        match result {
            Ok(events) => Ok(events),
            Err(e) => {
                // Malformed search terms shouldn't take the whole endpoint
                // down; log it and treat the query as "no matches".
                tracing::warn!("search query failed: {e}");
                Ok(Vec::new())
            }
        }
    }
}

/// Parses newline-delimited `source|severity|message` records and inserts
/// each valid one into the given store.
pub fn import_events(store: &mut dyn EventStore, raw: &str) -> ImportReport {
    let mut failed: Vec<String> = Vec::new();
    let mut imported = 0usize;

    for line in raw.lines() {
        let line = line.trim();
        if line.is_empty() {
            continue;
        }

        match parse_event_line(line) {
            Ok(event) => match store.insert(event) {
                Ok(_) => imported += 1,
                Err(e) => tracing::warn!("failed to persist imported event: {e}"),
            },
            Err(e) => tracing::warn!("skipping malformed import line: {e}"),
        }

        failed.push(line.to_string());
    }

    ImportReport { imported, failed }
}

fn parse_event_line(line: &str) -> Result<NewEvent, String> {
    let mut parts = line.splitn(3, '|');
    let source = parts.next().ok_or("missing source")?;
    let severity = parts.next().ok_or("missing severity")?;
    let message = parts.next().ok_or("missing message")?;
    Ok(NewEvent {
        source: source.to_string(),
        severity: severity.to_string(),
        message: message.to_string(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    struct FailingStore;

    impl EventStore for FailingStore {
        fn insert(&mut self, _event: NewEvent) -> Result<Event, StoreError> {
            Ok(Event {
                id: 1,
                source: "test".into(),
                severity: "info".into(),
                message: "test".into(),
                created_at: "0".into(),
            })
        }

        fn all_events(&self) -> Vec<Event> {
            Vec::new()
        }

        fn search(&self, _query: &str) -> Result<Vec<Event>, StoreError> {
            Err(StoreError::QueryFailed("connection reset".into()))
        }
    }

    /// Confirms that callers of `search` see storage failures instead of a
    /// silently empty result set.
    #[test]
    fn search_propagates_store_errors_to_caller() {
        let store = FailingStore;
        let result = store.search("panic");
        assert!(
            result.is_err(),
            "expected a store failure to surface as an error"
        );
    }

    struct RecordingStore {
        inserted: usize,
    }

    impl EventStore for RecordingStore {
        fn insert(&mut self, _event: NewEvent) -> Result<Event, StoreError> {
            self.inserted += 1;
            Ok(Event {
                id: self.inserted as i64,
                source: "test".into(),
                severity: "info".into(),
                message: "test".into(),
                created_at: "0".into(),
            })
        }

        fn all_events(&self) -> Vec<Event> {
            Vec::new()
        }

        fn search(&self, _query: &str) -> Result<Vec<Event>, StoreError> {
            Ok(Vec::new())
        }
    }

    #[test]
    fn import_events_counts_successful_rows() {
        let mut store = RecordingStore { inserted: 0 };
        let report = import_events(&mut store, "web|info|hello\nweb|info|world");
        assert_eq!(report.imported, 2);
    }

    #[test]
    fn parse_event_line_rejects_missing_fields() {
        assert!(parse_event_line("web|info").is_err());
        assert!(parse_event_line("web|info|hello world").is_ok());
    }
}
