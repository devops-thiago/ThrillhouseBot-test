use std::error::Error;

use erasure_processor::{ErasureProcessor, ErasureRequest, ErasureRequestSource, SubmissionStore};

/// A stand-in for the privacy platform used by the processing-pass tests
/// below.
struct FakeClient {
    requests: Vec<ErasureRequest>,
}

impl ErasureRequestSource for FakeClient {
    async fn fetch_pending_requests(&self) -> Result<Vec<ErasureRequest>, Box<dyn Error>> {
        Ok(self.requests.clone())
    }

    async fn submit_deletion(&self, _request_id: &str) -> Result<bool, Box<dyn Error>> {
        // Every submission succeeds against the fake platform.
        Ok(true)
    }
}

fn request(id: &str, user: &str, overdue_seconds: u64) -> ErasureRequest {
    ErasureRequest {
        request_id: id.to_string(),
        user_id: user.to_string(),
        overdue_seconds: Some(overdue_seconds),
        received_at: Some("2026-01-01T00:00:00Z".to_string()),
    }
}

#[tokio::test]
async fn overdue_requests_are_submitted_and_others_are_left_alone() {
    let requests = vec![
        request("r1", "user-a", 200_000), // well past the grace period
        request("r2", "user-b", 1_000),   // not yet actionable
    ];
    let client = FakeClient { requests };
    let store = SubmissionStore::open(":memory:").unwrap();
    let processor = ErasureProcessor::new(client, store, 86_400);

    let summary = processor.run().await.unwrap();

    assert_eq!(summary.checked, 2);
    assert_eq!(summary.submitted, 1);
}

#[tokio::test]
async fn duplicate_requests_for_the_same_user_only_count_once() {
    let requests = vec![
        request("r1", "user-a", 200_000),
        request("r2", "user-a", 250_000),
    ];
    let client = FakeClient { requests };
    let store = SubmissionStore::open(":memory:").unwrap();
    let processor = ErasureProcessor::new(client, store, 86_400);

    let summary = processor.run().await.unwrap();

    assert_eq!(summary.checked, 1);
}
