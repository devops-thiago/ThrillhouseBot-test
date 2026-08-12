use erasure_processor::{ErasureClient, ErasureProcessor, SubmissionStore};
use std::time::Duration;

/// Comma-separated list of user ids that are never auto-submitted for
/// deletion even when overdue, e.g. accounts under an active legal hold.
fn parse_exempt_accounts(raw: &str) -> Vec<String> {
    raw.split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect()
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let base_url = std::env::var("ERASURE_API_BASE_URL")
        .unwrap_or_else(|_| "http://localhost:8081".to_string());

    let submission_grace_secs: u64 = std::env::var("SUBMISSION_GRACE_SECS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(86_400);

    let db_path = std::env::var("SUBMISSION_DB_PATH")
        .unwrap_or_else(|_| "erasure_processor.db".to_string());

    let poll_interval_secs: u64 = std::env::var("POLL_INTERVAL_SECS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(300);

    let exempt_accounts = std::env::var("EXEMPT_ACCOUNT_IDS")
        .map(|v| parse_exempt_accounts(&v))
        .unwrap_or_default();

    println!(
        "erasure-processor starting: base_url={} submission_grace_secs={} poll_interval_secs={} exempt_accounts={}",
        base_url,
        submission_grace_secs,
        poll_interval_secs,
        exempt_accounts.len()
    );

    let client = ErasureClient::new(base_url);
    let store = SubmissionStore::open(&db_path)?;
    let processor = ErasureProcessor::new(client, store, submission_grace_secs);

    let mut ticker = tokio::time::interval(Duration::from_secs(poll_interval_secs));
    loop {
        ticker.tick().await;

        let summary = processor.run().await?;
        println!(
            "processing pass complete: checked={} submitted={}",
            summary.checked, summary.submitted
        );

        if !summary.failed_submissions.is_empty() {
            eprintln!(
                "WARNING: {} deletion submissions failed this pass, check the submission log",
                summary.failed_submissions.len()
            );
        }
    }
}
