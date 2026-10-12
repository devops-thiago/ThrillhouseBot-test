//! Runtime configuration read from the environment.

use std::path::PathBuf;
use std::time::Duration;

pub struct Config {
    pub bind_addr: String,
    pub archive_dir: PathBuf,
    pub backfill_sites: Vec<String>,
    pub backfill_interval: Duration,
}

impl Config {
    pub fn from_env() -> Self {
        let var = |key: &str, default: &str| std::env::var(key).unwrap_or_else(|_| default.to_string());
        let sites = var("INGEST_BACKFILL_SITES", "");
        let backfill_sites = sites.split(',').map(|s| s.trim().to_string()).filter(|s| !s.is_empty()).collect();
        let interval_secs = var("INGEST_BACKFILL_INTERVAL_SECS", "300").parse().unwrap_or(300);
        Config {
            bind_addr: var("INGEST_BIND_ADDR", "0.0.0.0:8080"),
            archive_dir: PathBuf::from(var("INGEST_ARCHIVE_DIR", "/var/lib/solar-ingest")),
            backfill_sites,
            backfill_interval: Duration::from_secs(interval_secs),
        }
    }
}
