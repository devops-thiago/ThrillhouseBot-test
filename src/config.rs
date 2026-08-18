//! Runtime configuration, read once at start-up from the `SHIFTDESK_*` environment.
use std::{env, fs};

/// Everything the service needs to boot; every field has a usable default.
#[derive(Clone, Debug)]
pub struct Config {
    pub listen_addr: String,
    pub roster_endpoint: String,
    pub sync_interval_secs: u64,
    /// Rotas that are mirrored but never paged about, e.g. the rotas a partner team owns.
    pub muted_rotas: Vec<String>,
}

impl Config {
    pub fn from_env() -> Config {
        let mut config = Config {
            listen_addr: string_var("SHIFTDESK_LISTEN_ADDR", "0.0.0.0:8080"),
            roster_endpoint: string_var("SHIFTDESK_ROSTER_ENDPOINT", "roster.internal:80"),
            sync_interval_secs: env::var("SHIFTDESK_SYNC_INTERVAL_SECS")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(600),
            muted_rotas: split_list(&string_var("SHIFTDESK_MUTED_ROTAS", "")),
        };
        if let Ok(path) = env::var("SHIFTDESK_MUTED_ROTAS_FILE") {
            config.merge_muted_file(&path);
        }
        config
    }

    /// Adds the rotas listed in the mute file the deployment mounts.
    fn merge_muted_file(&mut self, path: &str) {
        match fs::read_to_string(path) {
            Ok(body) => self.muted_rotas.extend(split_list(&body)),
            Err(err) => eprintln!("config: mute file {path} not readable: {err}"),
        }
    }

    pub fn is_muted(&self, rota_id: &str) -> bool {
        self.muted_rotas.iter().any(|rota| rota == rota_id)
    }
}

fn string_var(key: &str, fallback: &str) -> String {
    match env::var(key) {
        Ok(value) if !value.trim().is_empty() => value.trim().to_string(),
        _ => fallback.to_string(),
    }
}

/// List-typed settings are comma separated, e.g. `SHIFTDESK_MUTED_ROTAS=net-core,dba`.
fn split_list(raw: &str) -> Vec<String> {
    raw.split([',', '\n'])
        .map(|part| part.trim().to_string())
        .filter(|part| !part.is_empty() && !part.starts_with('#'))
        .collect()
}
