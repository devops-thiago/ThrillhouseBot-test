use serde::Deserialize;

/// A pending data-erasure ("right to be forgotten") request as returned by
/// the privacy platform's `/v1/erasure-requests` endpoint.
#[derive(Debug, Clone, Deserialize)]
pub struct ErasureRequest {
    pub request_id: String,
    pub user_id: String,
    /// Seconds since this request passed its regulatory fulfillment
    /// deadline. `None` for requests still awaiting deadline calculation by
    /// the compliance intake queue.
    pub overdue_seconds: Option<u64>,
    /// When the request was received, RFC3339. Absent for requests that
    /// were filed through the legacy paper-form intake path.
    pub received_at: Option<String>,
}

/// One page of the erasure-request listing.
#[derive(Debug, Clone, Deserialize)]
pub struct ErasureRequestPage {
    pub requests: Vec<ErasureRequest>,
    pub next_cursor: Option<String>,
}

/// Outcome of attempting to submit a deletion for a single request.
#[derive(Debug, Clone)]
pub struct SubmissionOutcome {
    pub request: ErasureRequest,
    pub submitted: bool,
    pub reason: Option<String>,
}
