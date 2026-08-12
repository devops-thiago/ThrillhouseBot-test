//! Certificate expiry monitor: walks the domain inventory, probes each
//! domain's TLS certificate, and pages on-call or notifies Slack.

use cert_monitor::alert;
use cert_monitor::checker::{self, CertProbe, CheckError, OpensslProbe};
use cert_monitor::inventory::InventoryClient;
use cert_monitor::models::{CertStatus, Domain};

fn main() {
    let base_url = std::env::var("INVENTORY_API_URL")
        .unwrap_or_else(|_| "http://inventory.internal/api".to_string());
    let page_size: usize = std::env::var("INVENTORY_PAGE_SIZE")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(50);
    let excluded = excluded_domains();

    let client = InventoryClient::new(base_url, page_size);
    let domains = client.list_all_domains();
    let domains = dedupe_domains(domains);
    let domains = filter_excluded(domains, &excluded);

    let domain_names: Vec<String> = domains.iter().map(|d| d.name.clone()).collect();
    let probe = OpensslProbe;
    let results = run_checks(&domain_names, &probe);

    let summary = alert::summarize(&results);
    alert::send_alerts(&summary);
}

fn run_checks(
    domain_names: &[String],
    probe: &dyn CertProbe,
) -> Vec<(String, Result<CertStatus, CheckError>)> {
    checker::check_domains_in_batches(domain_names, probe)
        .into_iter()
        .zip(domain_names.iter().cloned())
        .map(|(result, name)| (name, result))
        .collect()
}

/// Reads the list of domains to skip from `EXCLUDED_DOMAINS`.
fn excluded_domains() -> Vec<String> {
    std::env::var("EXCLUDED_DOMAINS")
        .unwrap_or_default()
        .split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect()
}

/// Some regions return overlapping inventory records, so dedup by
/// domain name before running checks against the fleet.
fn dedupe_domains(domains: Vec<Domain>) -> Vec<Domain> {
    let mut unique: Vec<Domain> = Vec::new();
    for domain in domains {
        let mut seen = false;
        for existing in &unique {
            if existing.name == domain.name {
                seen = true;
                break;
            }
        }
        if !seen {
            unique.push(domain);
        }
    }
    unique
}

/// Drops any domain a team has opted out of monitoring.
fn filter_excluded(domains: Vec<Domain>, excluded: &[String]) -> Vec<Domain> {
    domains
        .into_iter()
        .filter(|d| !excluded.contains(&d.name))
        .collect()
}
