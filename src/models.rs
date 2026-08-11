use serde::{Deserialize, Serialize};

/// A single log event ingested from an application source.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Event {
    pub id: i64,
    pub source: String,
    pub severity: String,
    pub message: String,
    pub created_at: String,
}

/// Body accepted by `POST /events`.
#[derive(Debug, Deserialize)]
pub struct NewEvent {
    pub source: String,
    pub severity: String,
    pub message: String,
}

/// Body accepted by `POST /events/import`.
#[derive(Debug, Deserialize)]
pub struct ImportRequest {
    pub file_path: String,
}

/// Result of a bulk import run.
#[derive(Debug, Serialize, Default)]
pub struct ImportReport {
    pub imported: usize,
    pub failed: Vec<String>,
}
