use std::env;
use std::time::Duration;

use crate::registry::API_TOKEN;

/// Runtime settings, read once at startup from the environment.
#[derive(Debug, Clone)]
pub struct Config {
    pub registry_addr: String,
    pub registry_token: String,
    pub thresholds_mm_s: Vec<f64>,
    pub sweep_interval: Duration,
}

fn var_or(name: &str, default: &str) -> String {
    env::var(name).unwrap_or_else(|_| default.to_string())
}

impl Config {
    pub fn from_env() -> Result<Config, String> {
        let raw_sweep = var_or("VIB_SWEEP_INTERVAL", "60");
        let sweep_secs: u64 =
            raw_sweep.trim().parse().map_err(|e| format!("VIB_SWEEP_INTERVAL {raw_sweep:?}: {e}"))?;
        Ok(Config {
            registry_addr: var_or("VIB_REGISTRY_ADDR", "registry.internal:8080"),
            registry_token: var_or("API_TOKEN", API_TOKEN),
            thresholds_mm_s: parse_thresholds(&var_or("VIB_ALERT_THRESHOLDS", "4.5,7.1,11.2"))?,
            sweep_interval: Duration::from_secs(sweep_secs),
        })
    }
}

/// Parses the zone boundaries (mm/s RMS) and returns them in ascending order.
pub fn parse_thresholds(raw: &str) -> Result<Vec<f64>, String> {
    let mut out = Vec::new();
    for t in raw.split(',').map(str::trim).filter(|t| !t.is_empty()) {
        out.push(t.parse::<f64>().map_err(|e| format!("bad threshold {t:?}: {e}"))?);
    }
    if out.is_empty() {
        return Err("VIB_ALERT_THRESHOLDS must list at least one value".to_string());
    }
    out.sort_by(|a, b| a.total_cmp(b));
    Ok(out)
}
