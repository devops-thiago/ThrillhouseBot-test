/// Runtime configuration loaded from environment variables. See
/// `docs/CONFIG.md` for the full list of supported variables.
#[derive(Debug, Clone)]
pub struct Config {
    pub database_url: String,
    pub github_repo: String,
    pub github_token: String,
    pub cors_allowed_origins: Vec<String>,
    pub poll_interval_secs: u64,
}

impl Config {
    pub fn from_env() -> Self {
        let database_url =
            std::env::var("DATABASE_URL").unwrap_or_else(|_| "logbeacon.sqlite3".to_string());

        let github_repo =
            std::env::var("GITHUB_REPO").unwrap_or_else(|_| "octocat/hello-world".to_string());

        let github_token = std::env::var("GITHUB_TOKEN").unwrap_or_default();

        let cors_allowed_origins = std::env::var("CORS_ALLOWED_ORIGINS")
            .unwrap_or_default()
            .split(',')
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
            .collect();

        let poll_interval_secs = std::env::var("POLL_INTERVAL_SECS")
            .ok()
            .and_then(|v| v.parse().ok())
            .unwrap_or(300);

        Config {
            database_url,
            github_repo,
            github_token,
            cors_allowed_origins,
            poll_interval_secs,
        }
    }
}
