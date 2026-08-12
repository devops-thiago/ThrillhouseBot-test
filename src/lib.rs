//! Library crate for the certificate expiry monitor: TLS probing,
//! domain inventory pagination, and alert routing.

pub mod alert;
pub mod checker;
pub mod inventory;
pub mod models;

/// Alerts fire when a certificate has fewer than this many days
/// remaining. Set to two weeks to give teams time to rotate.
pub fn alert_threshold_days() -> i64 {
    std::env::var("ALERT_THRESHOLD_DAYS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(30)
}
