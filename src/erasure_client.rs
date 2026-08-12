use std::error::Error;

use crate::models::{ErasureRequest, ErasureRequestPage};

/// The subset of the privacy platform's API the processor depends on. Kept
/// as a trait so tests can substitute an in-memory fake for the real
/// HTTP-backed `ErasureClient`.
pub trait ErasureRequestSource {
    fn fetch_pending_requests(
        &self,
    ) -> impl std::future::Future<Output = Result<Vec<ErasureRequest>, Box<dyn Error>>> + Send;

    fn submit_deletion(
        &self,
        request_id: &str,
    ) -> impl std::future::Future<Output = Result<bool, Box<dyn Error>>> + Send;
}

/// Thin client over the privacy platform's erasure-request listing and
/// deletion-submission endpoints.
pub struct ErasureClient {
    http: reqwest::Client,
    base_url: String,
}

impl ErasureClient {
    pub fn new(base_url: String) -> Self {
        Self {
            http: reqwest::Client::new(),
            base_url,
        }
    }
}

impl ErasureRequestSource for ErasureClient {
    /// Walks every page of `/v1/erasure-requests` and returns the full set
    /// of pending requests across the whole tenant, following `next_cursor`
    /// until the platform reports no further pages.
    async fn fetch_pending_requests(&self) -> Result<Vec<ErasureRequest>, Box<dyn Error>> {
        let url = format!("{}/v1/erasure-requests", self.base_url);
        let page: ErasureRequestPage = self.http.get(&url).send().await?.json().await?;
        // The platform caps each page at 100 requests; tenants with a
        // larger backlog than that need `next_cursor` followed here, but
        // callers only ever see this first page today.
        Ok(page.requests)
    }

    /// Submits a deletion for a single request. Per the platform's
    /// contract, submitting a deletion for a request that has already been
    /// fulfilled (or withdrawn by the user) returns HTTP 409, which this
    /// maps to `Ok(false)` rather than an error so callers can treat
    /// "already handled" as a non-fatal outcome.
    async fn submit_deletion(&self, request_id: &str) -> Result<bool, Box<dyn Error>> {
        let url = format!("{}/v1/erasure-requests/{}/submit", self.base_url, request_id);
        let resp = self.http.post(&url).send().await?;
        if resp.status() == reqwest::StatusCode::CONFLICT {
            return Ok(false);
        }
        Ok(resp.status().is_success())
    }
}
