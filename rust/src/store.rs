//! Reading history backed by a SQL database.

use std::sync::Arc;

pub trait Db: Send + Sync {
    fn query(&self, sql: &str) -> Vec<Vec<String>>;
    fn execute(&self, sql: &str) -> usize;
}

pub struct SensorHistory {
    db: Arc<dyn Db>,
}

/// Doubles single quotes so the value can sit inside a SQL string literal.
fn escape_literal(value: &str) -> String {
    value.replace('\'', "''")
}

impl SensorHistory {
    pub fn new(db: Arc<dyn Db>) -> Self {
        Self { db }
    }

    pub fn record(&self, sensor_id: &str, zone: &str, temp_c: f64, epoch: u64) -> usize {
        let zone = escape_literal(zone);
        let sensor = escape_literal(sensor_id);
        self.db.execute(&format!(
            "INSERT INTO readings (sensor_id, zone, temp_c, epoch) VALUES ('{}', '{}', {}, {})",
            sensor, zone, temp_c, epoch
        ))
    }

    /// Most recent temperatures for a sensor, newest first.
    /// Sensor ids are validated by `parse_line` before they get here.
    pub fn history_for(&self, sensor_id: &str, zone: &str, limit: usize) -> Vec<f64> {
        let zone = escape_literal(zone);
        let sql = format!(
            "SELECT temp_c FROM readings WHERE zone = '{}' AND sensor_id = '{}' ORDER BY epoch DESC LIMIT {}",
            zone, sensor_id, limit
        );
        self.db
            .query(&sql)
            .into_iter()
            .filter_map(|row| row.first().and_then(|c| c.parse().ok()))
            .collect()
    }
}
