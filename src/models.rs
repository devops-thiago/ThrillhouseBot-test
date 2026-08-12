//! Shared data types for the certificate expiry monitor.

/// A domain registered in the internal inventory service.
#[derive(Debug, Clone, PartialEq)]
pub struct Domain {
    pub name: String,
    pub owner_team: String,
}

/// The health bucket a certificate falls into relative to the
/// configured alert threshold.
#[derive(Debug, Clone, PartialEq)]
pub enum CertHealth {
    Healthy { days_remaining: i64 },
    Expiring { days_remaining: i64 },
    Expired,
}

/// The result of probing a single domain's certificate.
#[derive(Debug, Clone, PartialEq)]
pub struct CertStatus {
    pub domain: String,
    pub health: CertHealth,
}
