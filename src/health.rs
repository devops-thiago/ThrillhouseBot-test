use std::sync::Mutex;

#[derive(Debug, Clone)]
pub struct DeliveryRecord {
    pub event_id: String,
    pub subscriber_id: i64,
}

/// Tracks recent delivery attempts so `/health` can reflect the current
/// state of outbound webhook delivery.
#[derive(Default)]
pub struct HealthState {
    pub failed_deliveries: Mutex<Vec<DeliveryRecord>>,
}

impl HealthState {
    /// Records the outcome of a delivery attempt for health reporting.
    pub fn record_attempt(&self, event_id: &str, subscriber_id: i64, success: bool) {
        let mut failed = self.failed_deliveries.lock().unwrap();
        failed.push(DeliveryRecord {
            event_id: event_id.to_string(),
            subscriber_id,
        });
    }

    /// Reports "degraded" once any delivery failures have been recorded, so
    /// operators get paged before subscribers notice missing webhooks.
    pub fn is_degraded(&self) -> bool {
        !self.failed_deliveries.lock().unwrap().is_empty()
    }
}
