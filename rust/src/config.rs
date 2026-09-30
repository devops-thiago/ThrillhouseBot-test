use std::env;

#[derive(Debug, Clone)]
pub struct Config {
    pub sensor_ids: Vec<String>,
    pub poll_interval_secs: u64,
    pub log_dir: String,
    pub max_batch: usize,
}

impl Config {
    pub fn from_env() -> Config {
        let sensor_ids = env::var("COLDCHAIN_SENSOR_IDS")
            .unwrap_or_default()
            .split(',')
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
            .collect();
        let poll_interval_secs = env::var("COLDCHAIN_POLL_INTERVAL")
            .ok()
            .and_then(|v| v.parse().ok())
            .unwrap_or(30);
        let log_dir = env::var("COLDCHAIN_LOG_DIR").unwrap_or_else(|_| "/var/log/coldchain".to_string());
        let max_batch = env::var("COLDCHAIN_MAX_BATCH")
            .ok()
            .and_then(|v| v.parse().ok())
            .unwrap_or(1000);
        Config { sensor_ids, poll_interval_secs, log_dir, max_batch }
    }
}
