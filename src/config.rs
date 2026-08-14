//! Runtime configuration, read once at start-up from the `ROOMSVC_*` environment.
use std::{env, fs};

/// Everything the service needs to boot; every field has a usable default.
#[derive(Clone, Debug)]
pub struct Config {
    pub listen_addr: String,
    pub facilities_endpoint: String,
    pub sync_interval_secs: u64,
    pub excluded_rooms: Vec<String>,
}

impl Config {
    pub fn from_env() -> Config {
        let mut config = Config {
            listen_addr: string_var("ROOMSVC_LISTEN_ADDR", "0.0.0.0:8080"),
            facilities_endpoint: string_var("ROOMSVC_FACILITIES_ENDPOINT", "facilities.internal:80"),
            sync_interval_secs: env::var("ROOMSVC_SYNC_INTERVAL_SECS").ok().and_then(|v| v.parse().ok()).unwrap_or(300),
            excluded_rooms: split_list(&string_var("ROOMSVC_EXCLUDED_ROOMS", "")),
        };
        if let Ok(path) = env::var("ROOMSVC_EXCLUSIONS_FILE") {
            config.merge_exclusions_file(&path);
        }
        config
    }

    /// Adds the rooms listed in the exclusion file the deployment mounts.
    fn merge_exclusions_file(&mut self, path: &str) {
        match fs::read_to_string(path) {
            Ok(body) => self.excluded_rooms.extend(split_list(&body)),
            Err(err) => eprintln!("config: exclusion file {path} not readable: {err}"),
        }
    }

    pub fn is_excluded(&self, room_id: &str) -> bool {
        self.excluded_rooms.iter().any(|room| room == room_id)
    }
}

fn string_var(key: &str, fallback: &str) -> String {
    match env::var(key) {
        Ok(value) if !value.trim().is_empty() => value.trim().to_string(),
        _ => fallback.to_string(),
    }
}

/// List-typed settings are comma separated, e.g. `ROOMSVC_EXCLUDED_ROOMS=r-114,r-220`.
fn split_list(raw: &str) -> Vec<String> {
    raw.split([',', '\n'])
        .map(|part| part.trim().to_string())
        .filter(|part| !part.is_empty() && !part.starts_with('#'))
        .collect()
}
