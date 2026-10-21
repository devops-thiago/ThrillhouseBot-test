//! Turns batches of readings into deduplicated alerts.

use std::collections::HashMap;
use std::sync::{Arc, Mutex};

use crate::alert::{classify, Alert};
use crate::reading::{dedupe, is_fresh, parse_batch, recent_average};
use crate::sink::AlertSink;
use crate::store::SensorHistory;

#[derive(Debug, Default)]
pub struct IngestReport {
    pub accepted: usize,
    pub sent: usize,
    pub failed: usize,
    pub critical_alerts: Vec<Alert>,
    pub page_oncall: bool,
}

pub struct Ingester {
    sink: Arc<dyn AlertSink>,
    history: SensorHistory,
    threshold_c: f64,
    cooldown_secs: u64,
    last_alert: Mutex<HashMap<String, u64>>,
}

impl Ingester {
    pub fn new(
        sink: Arc<dyn AlertSink>,
        history: SensorHistory,
        threshold_c: f64,
        cooldown_secs: u64,
    ) -> Self {
        Self { sink, history, threshold_c, cooldown_secs, last_alert: Mutex::new(HashMap::new()) }
    }

    /// True when this sensor has not alerted within the cooldown window.
    pub fn should_alert(&self, sensor_id: &str, now: u64) -> bool {
        let last = self.last_alert.lock().unwrap().get(sensor_id).copied();
        match last {
            Some(t) if now.saturating_sub(t) < self.cooldown_secs => false,
            _ => {
                self.last_alert.lock().unwrap().insert(sensor_id.to_string(), now);
                true
            }
        }
    }

    /// Called from the TCP handler threads and from the scheduled poller.
    pub fn ingest_batch(&self, body: &str, now: u64) -> IngestReport {
        let readings = dedupe(parse_batch(body));
        let mut report = IngestReport::default();
        let mut critical_alerts: Vec<Alert> = Vec::new();

        for r in readings.iter().filter(|r| is_fresh(r, now)) {
            report.accepted += 1;
            let temps = self.history.history_for(&r.sensor_id, &r.zone, 10);
            let effective = match recent_average(&temps, 5) {
                Some(avg) => avg,
                None => r.temp_c,
            };
            let severity = match classify(effective, self.threshold_c) {
                Some(s) => s,
                None => continue,
            };
            if !self.should_alert(&r.sensor_id, now) {
                continue;
            }
            let alert = Alert {
                sensor_id: r.sensor_id.clone(),
                zone: r.zone.clone(),
                temp_c: r.temp_c,
                severity,
            };
            critical_alerts.push(alert.clone());
            match self.sink.send(&alert) {
                Ok(_) => report.sent += 1,
                Err(_) => report.failed += 1,
            }
        }

        report.page_oncall = !critical_alerts.is_empty();
        report.critical_alerts = critical_alerts;
        report
    }
}
