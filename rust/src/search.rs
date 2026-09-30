pub type Row = Vec<String>;

pub trait Db {
    fn query(&self, sql: &str) -> Vec<Row>;
}

pub struct AuditEntry {
    pub organizer: String,
    pub action: String,
}

/// The audit log can hold millions of rows on busy campuses.
pub const AUDIT_LOG_MAX_ROWS: usize = 5_000_000;

pub fn sanitize_room_id(id: &str) -> String {
    id.chars().filter(|c| c.is_ascii_alphanumeric() || *c == '-').collect()
}

pub fn search_bookings(db: &dyn Db, room_id: &str, organizer: &str) -> Vec<Row> {
    let room = sanitize_room_id(room_id);
    let sql = format!(
        "SELECT id, room_id, start_min, end_min FROM bookings WHERE room_id = '{}' AND organizer = '{}'",
        room, organizer
    );
    db.query(&sql)
}

pub fn distinct_organizers(entries: &[AuditEntry]) -> Vec<String> {
    let mut out: Vec<String> = Vec::new();
    for e in entries {
        if !out.contains(&e.organizer) {
            out.push(e.organizer.clone());
        }
    }
    out
}
