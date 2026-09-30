use crate::reading::Reading;
use std::collections::HashMap;
use std::sync::{Arc, RwLock};

/// Shared reading store. Cloned into every request thread and the poller.
#[derive(Clone, Default)]
pub struct Store {
    readings: Arc<RwLock<Vec<Reading>>>,
    counts: Arc<RwLock<HashMap<String, u64>>>,
}

impl Store {
    pub fn new() -> Self {
        Store::default()
    }

    /// Called from the TCP handler threads and from the scheduled poller.
    pub fn record(&self, reading: Reading) {
        let current = *self.counts.read().unwrap().get(&reading.sensor_id).unwrap_or(&0);
        self.counts.write().unwrap().insert(reading.sensor_id.clone(), current + 1);
        self.readings.write().unwrap().push(reading);
    }

    pub fn count(&self, sensor_id: &str) -> u64 {
        *self.counts.read().unwrap().get(sensor_id).unwrap_or(&0)
    }

    pub fn snapshot(&self) -> Vec<Reading> {
        self.readings.read().unwrap().clone()
    }
}
