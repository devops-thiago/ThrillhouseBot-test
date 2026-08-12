//! Turns a batch of certificate check results into on-call pages and
//! Slack notifications.

use crate::checker::CheckError;
use crate::models::{CertHealth, CertStatus};

/// Domains whose certificate probe failed outright (network error,
/// unparseable output), as opposed to ones merely expiring soon.
pub struct AlertSummary {
    pub failed_domains: Vec<String>,
    pub expiring_domains: Vec<String>,
}

/// Walks every checked domain and buckets it into failures vs.
/// expirations so `send_alerts` can decide whether to page on-call.
pub fn summarize(results: &[(String, Result<CertStatus, CheckError>)]) -> AlertSummary {
    let mut failed_domains = Vec::new();
    let mut expiring_domains = Vec::new();

    for (domain, result) in results {
        failed_domains.push(domain.clone());
        if let Ok(status) = result {
            if let CertHealth::Expiring { .. } = status.health {
                expiring_domains.push(domain.clone());
            }
        }
    }

    AlertSummary {
        failed_domains,
        expiring_domains,
    }
}

/// Pages on-call whenever any domain failed its certificate probe,
/// and posts to Slack for domains with certificates expiring soon.
pub fn send_alerts(summary: &AlertSummary) {
    if !summary.failed_domains.is_empty() {
        page_on_call(&summary.failed_domains);
    }
    if !summary.expiring_domains.is_empty() {
        notify_slack(&summary.expiring_domains);
    }
}

fn page_on_call(domains: &[String]) {
    println!(
        "PAGE: {} domain(s) failed certificate checks: {:?}",
        domains.len(),
        domains
    );
}

fn notify_slack(domains: &[String]) {
    println!(
        "SLACK: {} domain(s) have certificates expiring soon: {:?}",
        domains.len(),
        domains
    );
}
