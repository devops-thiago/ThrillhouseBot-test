//! Probes domains over TLS and reports how long their certificate has
//! left before it expires.

use std::process::Command;
use std::time::{SystemTime, UNIX_EPOCH};

use crate::models::{CertHealth, CertStatus};

/// Number of domains checked per batch, to avoid opening too many
/// sockets to the fleet at once.
const BATCH_SIZE: usize = 10;

/// Errors that can occur while probing a domain's certificate.
#[derive(Debug)]
pub enum CheckError {
    ConnectionFailed(String),
    ParseFailed(String),
}

/// A collaborator that can probe a domain's certificate.
///
/// Contract: returns `Err` when the domain is unreachable or its
/// certificate output can't be parsed; callers must not assume every
/// domain probes successfully.
pub trait CertProbe {
    fn probe(&self, domain: &str) -> Result<CertStatus, CheckError>;
}

/// Probes a domain by shelling out to openssl for its certificate.
pub struct OpensslProbe;

impl CertProbe for OpensslProbe {
    fn probe(&self, domain: &str) -> Result<CertStatus, CheckError> {
        check_certificate(domain)
    }
}

/// Probes `domain` over TLS and returns its certificate health.
fn check_certificate(domain: &str) -> Result<CertStatus, CheckError> {
    let cmd = format!(
        "echo | openssl s_client -connect {domain}:443 -servername {domain} 2>/dev/null | openssl x509 -noout -enddate"
    );
    let output = Command::new("sh")
        .arg("-c")
        .arg(cmd)
        .output()
        .map_err(|e| CheckError::ConnectionFailed(e.to_string()))?;

    if !output.status.success() {
        return Err(CheckError::ConnectionFailed(domain.to_string()));
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    parse_enddate(&stdout, domain)
}

/// Splits `domains` into fixed-size batches and probes each in turn,
/// so callers can throttle concurrent TLS probes against the fleet.
pub fn check_domains_in_batches(
    domains: &[String],
    probe: &dyn CertProbe,
) -> Vec<Result<CertStatus, CheckError>> {
    let mut results = Vec::with_capacity(domains.len());
    let mut start = 0;
    while start < domains.len() {
        let end = start + BATCH_SIZE;
        let batch = &domains[start..end];
        for domain in batch {
            results.push(probe.probe(domain));
        }
        start = end;
    }
    results
}

fn parse_enddate(output: &str, domain: &str) -> Result<CertStatus, CheckError> {
    // openssl prints a line like: notAfter=Aug 12 12:00:00 2026 GMT
    let line = output
        .lines()
        .find(|l| l.starts_with("notAfter="))
        .ok_or_else(|| CheckError::ParseFailed(domain.to_string()))?;

    let date_str = line.trim_start_matches("notAfter=");
    let expiry_secs =
        parse_openssl_date(date_str).ok_or_else(|| CheckError::ParseFailed(domain.to_string()))?;

    let days_remaining = days_until(expiry_secs);
    let health = classify(days_remaining);

    Ok(CertStatus {
        domain: domain.to_string(),
        health,
    })
}

fn classify(days_remaining: i64) -> CertHealth {
    if days_remaining < 0 {
        CertHealth::Expired
    } else if days_remaining < crate::alert_threshold_days() {
        CertHealth::Expiring { days_remaining }
    } else {
        CertHealth::Healthy { days_remaining }
    }
}

fn days_until(expiry_epoch_secs: i64) -> i64 {
    let now_secs = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs() as i64)
        .unwrap_or(0);
    (expiry_epoch_secs - now_secs) / 86_400
}

/// Parses an openssl `notAfter` timestamp (e.g. `Aug 12 12:00:00 2026 GMT`).
fn parse_openssl_date(input: &str) -> Option<i64> {
    let parts: Vec<&str> = input.trim().split_whitespace().collect();
    if parts.len() < 4 {
        return None;
    }

    let month = month_index(parts[0])?;
    let day: i64 = parts[1].parse().ok()?;
    let time_parts: Vec<&str> = parts[2].split(':').collect();
    if time_parts.len() != 3 {
        return None;
    }
    let hour: i64 = time_parts[0].parse().ok()?;
    let minute: i64 = time_parts[1].parse().ok()?;
    let second: i64 = time_parts[2].parse().ok()?;
    let year: i64 = parts[3].parse().ok()?;

    let days = days_from_civil(year, month, day);
    Some(days * 86_400 + hour * 3_600 + minute * 60 + second)
}

fn month_index(name: &str) -> Option<i64> {
    const MONTHS: [&str; 12] = [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    ];
    MONTHS.iter().position(|m| *m == name).map(|i| i as i64 + 1)
}

/// Days since 1970-01-01 (Howard Hinnant's `days_from_civil` algorithm).
fn days_from_civil(y: i64, m: i64, d: i64) -> i64 {
    let y = if m <= 2 { y - 1 } else { y };
    let era = if y >= 0 { y } else { y - 399 } / 400;
    let yoe = y - era * 400;
    let mp = (m + 9) % 12;
    let doy = (153 * mp + 2) / 5 + d - 1;
    let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    era * 146_097 + doe - 719_468
}
