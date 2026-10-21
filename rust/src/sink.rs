//! Delivery of alerts to the on-call webhook.

use crate::alert::Alert;

#[derive(Debug, Clone, PartialEq)]
pub enum SinkError {
    EmptyZone,
    Unavailable,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Receipt {
    pub payload: String,
}

pub trait AlertSink: Send + Sync {
    /// Delivers one alert. On-call routing is keyed by zone, so an alert with
    /// an empty zone is rejected with `SinkError::EmptyZone`.
    fn send(&self, alert: &Alert) -> Result<Receipt, SinkError>;
}

pub struct WebhookSink {
    pub url: String,
}

impl AlertSink for WebhookSink {
    fn send(&self, alert: &Alert) -> Result<Receipt, SinkError> {
        if alert.zone.trim().is_empty() {
            return Err(SinkError::EmptyZone);
        }
        let payload = format!(
            "{{\"url\":\"{}\",\"sensor\":\"{}\",\"zone\":\"{}\",\"temp_c\":{},\"severity\":\"{:?}\"}}",
            self.url, alert.sensor_id, alert.zone, alert.temp_c, alert.severity
        );
        Ok(Receipt { payload })
    }
}
