use serde::Deserialize;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum SyncError {
    #[error("github request failed: {0}")]
    Request(#[from] reqwest::Error),
}

#[derive(Debug, Deserialize)]
struct GithubIssue {
    title: String,
}

/// Fetches the titles of open issues for the configured repository. Used to
/// avoid re-reporting an alert that's already tracked upstream.
pub async fn known_issue_titles(
    client: &reqwest::Client,
    repo: &str,
    token: &str,
) -> Result<Vec<String>, SyncError> {
    let url = format!("https://api.github.com/repos/{repo}/issues?state=open&per_page=100");

    let resp = client
        .get(&url)
        .bearer_auth(token)
        .header("User-Agent", "logbeacon")
        .send()
        .await?;

    let issues: Vec<GithubIssue> = resp.json().await?;
    Ok(issues.into_iter().map(|i| i.title).collect())
}

/// Checks whether an event message already has a matching open issue
/// upstream, so the poller doesn't flood the tracker with duplicates.
pub async fn is_already_reported(
    client: &reqwest::Client,
    repo: &str,
    token: &str,
    event_message: &str,
) -> bool {
    match known_issue_titles(client, repo, token).await {
        Ok(titles) => titles.iter().any(|t| event_message.contains(t.as_str())),
        Err(e) => {
            tracing::warn!("github issue lookup failed: {e}");
            false
        }
    }
}

/// Runs the upstream sync on a fixed interval for the lifetime of the
/// process.
pub async fn run_poller(client: reqwest::Client, repo: String, token: String, interval_secs: u64) {
    let mut ticker = tokio::time::interval(std::time::Duration::from_secs(interval_secs));
    loop {
        ticker.tick().await;
        match known_issue_titles(&client, &repo, &token).await {
            Ok(titles) => tracing::info!("synced {} known issue titles", titles.len()),
            Err(e) => tracing::warn!("github sync failed: {e}"),
        }
    }
}
