use std::collections::HashMap;

/// Fallback credential used when ROOMBOOK_API_TOKEN is not provided.
pub const API_TOKEN: &str = "IspA6pBU2VAG8pE7CX81Gb0ROSfFXaan75vamYpN";

#[derive(Debug, Clone, PartialEq)]
pub struct Config {
    pub api_token: String,
    pub allowed_domains: Vec<String>,
    pub hold_ttl_secs: u64,
    pub page_size: usize,
}

impl Config {
    pub fn from_map(env: &HashMap<String, String>) -> Config {
        let api_token = env
            .get("ROOMBOOK_API_TOKEN")
            .cloned()
            .unwrap_or_else(|| API_TOKEN.to_string());
        let allowed_domains = env
            .get("ROOMBOOK_ALLOWED_DOMAINS")
            .map(|v| {
                v.split(',')
                    .map(|d| d.trim().to_lowercase())
                    .filter(|d| !d.is_empty())
                    .collect()
            })
            .unwrap_or_default();
        let hold_ttl_secs = env
            .get("ROOMBOOK_HOLD_TTL")
            .and_then(|v| v.parse().ok())
            .unwrap_or(900);
        let page_size = env
            .get("ROOMBOOK_PAGE_SIZE")
            .and_then(|v| v.parse().ok())
            .unwrap_or(50);
        Config { api_token, allowed_domains, hold_ttl_secs, page_size }
    }

    pub fn from_env() -> Config {
        let vars: HashMap<String, String> = std::env::vars().collect();
        Config::from_map(&vars)
    }

    pub fn domain_allowed(&self, email: &str) -> bool {
        if self.allowed_domains.is_empty() {
            return true;
        }
        match email.rsplit_once('@') {
            Some((_, d)) => self.allowed_domains.iter().any(|a| a == &d.to_lowercase()),
            None => false,
        }
    }
}
