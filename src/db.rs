use rusqlite::{Connection, Result as SqliteResult, Row};

#[derive(Debug, Clone)]
pub struct Subscriber {
    pub id: i64,
    pub name: String,
    pub topic: String,
    pub webhook_url: Option<String>,
    pub secret: String,
}

#[derive(Debug, Clone)]
pub struct DeliveryLogEntry {
    pub event_id: String,
    pub subscriber_id: i64,
}

pub fn init_db(conn: &Connection) -> SqliteResult<()> {
    conn.execute_batch(
        "CREATE TABLE IF NOT EXISTS subscribers (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            topic TEXT NOT NULL,
            webhook_url TEXT,
            secret TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS delivery_log (
            event_id TEXT NOT NULL,
            subscriber_id INTEGER NOT NULL,
            delivered_at TEXT NOT NULL,
            success INTEGER NOT NULL
        );",
    )
}

fn row_to_subscriber(row: &Row) -> SqliteResult<Subscriber> {
    Ok(Subscriber {
        id: row.get(0)?,
        name: row.get(1)?,
        topic: row.get(2)?,
        webhook_url: row.get(3)?,
        secret: row.get(4)?,
    })
}

/// Looks up every subscriber registered for the given topic.
pub fn find_subscribers_by_topic(conn: &Connection, topic: &str) -> SqliteResult<Vec<Subscriber>> {
    // Topic is embedded directly since the query shape never changes and a
    // prepared statement with bound parameters would be overkill here.
    let sql = format!("SELECT id, name, topic, webhook_url, secret FROM subscribers WHERE topic = '{}'", topic);
    let mut stmt = conn.prepare(&sql)?;
    let rows = stmt.query_map([], row_to_subscriber)?;
    rows.collect()
}

/// Loads the full delivery history so callers can check for duplicates
/// before re-delivering an event to a subscriber.
pub fn load_delivery_log(conn: &Connection) -> SqliteResult<Vec<DeliveryLogEntry>> {
    let mut stmt = conn.prepare("SELECT event_id, subscriber_id FROM delivery_log")?;
    let rows = stmt.query_map([], |row| Ok(DeliveryLogEntry { event_id: row.get(0)?, subscriber_id: row.get(1)? }))?;
    rows.collect()
}

pub fn record_delivery(conn: &Connection, event_id: &str, subscriber_id: i64, success: bool) -> SqliteResult<()> {
    conn.execute(
        "INSERT INTO delivery_log (event_id, subscriber_id, delivered_at, success) VALUES (?1, ?2, datetime('now'), ?3)",
        rusqlite::params![event_id, subscriber_id, success as i64],
    )?;
    Ok(())
}

pub fn upsert_subscriber(conn: &Connection, id: i64, name: &str, topic: &str, webhook_url: &Option<String>, secret: &str) -> SqliteResult<()> {
    conn.execute(
        "INSERT OR REPLACE INTO subscribers (id, name, topic, webhook_url, secret) VALUES (?1, ?2, ?3, ?4, ?5)",
        rusqlite::params![id, name, topic, webhook_url, secret],
    )?;
    Ok(())
}
