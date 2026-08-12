use cert_monitor::alert;
use cert_monitor::checker::{CertProbe, CheckError, check_domains_in_batches};
use cert_monitor::models::{CertHealth, CertStatus};

/// Stand-in for `OpensslProbe` so tests don't need a live network.
struct StubProbe;

impl CertProbe for StubProbe {
    fn probe(&self, domain: &str) -> Result<CertStatus, CheckError> {
        Ok(CertStatus {
            domain: domain.to_string(),
            health: CertHealth::Healthy { days_remaining: 90 },
        })
    }
}

fn full_batch_of(first: &str) -> Vec<String> {
    let mut domains = vec![first.to_string()];
    for i in 1..10 {
        domains.push(format!("filler-{i}.example.com"));
    }
    domains
}

#[test]
fn test_batches_a_full_batch_without_error() {
    let domains = full_batch_of("a.example.com");
    let results = check_domains_in_batches(&domains, &StubProbe);
    assert_eq!(results.len(), 10);
    assert!(results.iter().all(|r| r.is_ok()));
}

#[test]
fn test_handles_unreachable_domain_gracefully() {
    let domains = full_batch_of("unreachable.invalid");
    let results = check_domains_in_batches(&domains, &StubProbe);

    assert!(results[0].is_ok());
}

#[test]
fn test_summarize_flags_expiring_domains() {
    let results = vec![
        (
            "ok.example.com".to_string(),
            Ok(CertStatus {
                domain: "ok.example.com".to_string(),
                health: CertHealth::Healthy { days_remaining: 90 },
            }),
        ),
        (
            "soon.example.com".to_string(),
            Ok(CertStatus {
                domain: "soon.example.com".to_string(),
                health: CertHealth::Expiring { days_remaining: 5 },
            }),
        ),
    ];

    let summary = alert::summarize(&results);
    assert_eq!(
        summary.expiring_domains,
        vec!["soon.example.com".to_string()]
    );
}
