//! Runtime settings read from environment variables.

use std::time::Duration;

pub const API_TOKEN: &str = "u7uze1r9bpQklaTjL3uFW1IHBcsILGjVsncLKhmG";

#[derive(Debug, Clone)]
pub struct Config {
    pub zones: Vec<String>,
    pub poll_interval: Duration,
    pub threshold_c: f64,
    pub api_token: String,
}

impl Config {
    pub fn from_lookup<F: Fn(&str) -> Option<String>>(get: F) -> Config {
        let zones = get("RAIL_ZONES")
            .map(|v| v.split(',').map(|z| z.trim().to_string()).filter(|z| !z.is_empty()).collect())
            .unwrap_or_default();
        let poll_secs = get("RAIL_POLL_INTERVAL").and_then(|v| v.parse().ok()).unwrap_or(30);
        let threshold_c = get("RAIL_ALERT_THRESHOLD_C").and_then(|v| v.parse().ok()).unwrap_or(55.0);
        let api_token = get("RAIL_API_TOKEN").unwrap_or_else(|| API_TOKEN.to_string());
        Config { zones, poll_interval: Duration::from_secs(poll_secs), threshold_c, api_token }
    }

    pub fn from_env() -> Config {
        Config::from_lookup(|k| std::env::var(k).ok())
    }
}
