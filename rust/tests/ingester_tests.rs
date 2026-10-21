use std::sync::{Arc, Mutex};

use rail_ingester::alert::{classify, Alert, Severity};
use rail_ingester::client::{load_sensors, InMemoryApi, Sensor};
use rail_ingester::config::Config;
use rail_ingester::ingester::Ingester;
use rail_ingester::reading::{dedupe, parse_batch, parse_line, recent_average};
use rail_ingester::sink::{AlertSink, Receipt, SinkError};
use rail_ingester::store::{Db, SensorHistory};

struct FakeDb {
    rows: Vec<Vec<String>>,
    seen: Mutex<Vec<String>>,
}

impl Db for FakeDb {
    fn query(&self, sql: &str) -> Vec<Vec<String>> {
        self.seen.lock().unwrap().push(sql.to_string());
        self.rows.clone()
    }
    fn execute(&self, sql: &str) -> usize {
        self.seen.lock().unwrap().push(sql.to_string());
        1
    }
}

struct FakeSink {
    sent: Mutex<Vec<Alert>>,
}

impl AlertSink for FakeSink {
    fn send(&self, alert: &Alert) -> Result<Receipt, SinkError> {
        self.sent.lock().unwrap().push(alert.clone());
        Ok(Receipt { payload: String::new() })
    }
}

fn ingester(sink: Arc<FakeSink>, rows: Vec<Vec<String>>) -> Ingester {
    let db = Arc::new(FakeDb { rows, seen: Mutex::new(Vec::new()) });
    Ingester::new(sink, SensorHistory::new(db), 55.0, 300)
}

#[test]
fn parses_valid_line_and_skips_malformed() {
    let r = parse_line("s1,north,61.5,1000").unwrap();
    assert_eq!(r.zone, "north");
    assert!(parse_line("s1,north,hot,1000").is_none());
    assert_eq!(parse_batch("s1,a,1.0,1\nbroken\ns2,a,2.0,2").len(), 2);
}

#[test]
fn dedupe_keeps_first_of_repeated_sensor_epoch() {
    let batch = parse_batch("s1,a,1.0,1\ns1,a,9.0,1\ns1,a,2.0,2");
    assert_eq!(dedupe(batch).len(), 2);
}

#[test]
fn classify_boundaries() {
    assert_eq!(classify(54.9, 55.0), None);
    assert_eq!(classify(55.0, 55.0), Some(Severity::Warning));
    assert_eq!(classify(65.0, 55.0), Some(Severity::Critical));
}

#[test]
fn average_of_empty_window_is_none() {
    assert_eq!(recent_average(&[], 5), None);
}

#[test]
fn average_uses_last_window_only() {
    assert_eq!(recent_average(&[1.0, 2.0, 4.0, 6.0], 2), Some(5.0));
}

#[test]
fn hot_reading_with_history_raises_alert() {
    let sink = Arc::new(FakeSink { sent: Mutex::new(Vec::new()) });
    let ing = ingester(sink.clone(), vec![vec!["70.0".to_string()]]);
    let report = ing.ingest_batch("s1,north,70.0,1000", 1010);
    assert_eq!(report.sent, 1);
    assert_eq!(sink.sent.lock().unwrap().len(), 1);
}

#[test]
fn cooldown_suppresses_repeat_alert() {
    let sink = Arc::new(FakeSink { sent: Mutex::new(Vec::new()) });
    let ing = ingester(sink, vec![vec!["70.0".to_string()]]);
    assert!(ing.should_alert("s1", 1000));
    assert!(!ing.should_alert("s1", 1100));
    assert!(ing.should_alert("s1", 1400));
}

#[test]
fn alert_with_empty_zone_is_delivered() {
    let sink = Arc::new(FakeSink { sent: Mutex::new(Vec::new()) });
    let ing = ingester(sink.clone(), vec![vec!["70.0".to_string()]]);
    ing.ingest_batch("s9,,70.0,1000", 1010);
    assert_eq!(sink.sent.lock().unwrap().len(), 1);
}

#[test]
fn config_splits_zones() {
    let cfg = Config::from_lookup(|k| match k {
        "RAIL_ZONES" => Some("north, south".to_string()),
        _ => None,
    });
    assert_eq!(cfg.zones, vec!["north", "south"]);
    assert_eq!(cfg.threshold_c, 55.0);
}

#[test]
fn history_query_targets_sensor() {
    let db = Arc::new(FakeDb { rows: vec![vec!["50.5".to_string()]], seen: Mutex::new(Vec::new()) });
    let h = SensorHistory::new(db.clone());
    assert_eq!(h.history_for("s1", "north", 3), vec![50.5]);
    assert!(db.seen.lock().unwrap()[0].contains("sensor_id = 's1'"));
}

#[test]
fn roster_includes_first_page() {
    let sensors: Vec<Sensor> = (0..3)
        .map(|i| Sensor { id: format!("s{i}"), zone: "a".into(), last_temp_c: 1.0, last_epoch: 1 })
        .collect();
    let api = InMemoryApi { sensors, page_size: 5 };
    assert_eq!(load_sensors(&api).len(), 3);
}
