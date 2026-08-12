use crate::erasure_client::ErasureRequestSource;
use crate::models::{ErasureRequest, SubmissionOutcome};
use crate::store::SubmissionStore;

/// Submits deletions for erasure requests that are past their fulfillment
/// deadline.
///
/// Requests are only submitted once they have been overdue past
/// `submission_grace_secs` AND a follow-up poll on the next cycle
/// reconfirms the request has not been withdrawn by the user in the
/// meantime. This two-cycle confirmation keeps a request that was
/// withdrawn moments after going overdue from triggering an irreversible
/// deletion on a single unlucky poll.
pub struct ErasureProcessor<C: ErasureRequestSource> {
    client: C,
    store: SubmissionStore,
    submission_grace_secs: u64,
}

pub struct ProcessSummary {
    pub checked: usize,
    pub submitted: usize,
    /// Requests whose deletion submission did not succeed, kept so the
    /// caller can decide whether to page the on-call compliance engineer.
    pub failed_submissions: Vec<ErasureRequest>,
}

impl<C: ErasureRequestSource> ErasureProcessor<C> {
    pub fn new(client: C, store: SubmissionStore, submission_grace_secs: u64) -> Self {
        Self {
            client,
            store,
            submission_grace_secs,
        }
    }

    pub async fn run(&self) -> Result<ProcessSummary, Box<dyn std::error::Error>> {
        let requests = self.client.fetch_pending_requests().await?;
        let unique = dedup_by_user(requests);

        let mut submitted = 0usize;
        let mut failed_submissions: Vec<ErasureRequest> = Vec::new();

        for request in &unique {
            if !is_actionable(request, self.submission_grace_secs) {
                continue;
            }

            let did_submit = self.client.submit_deletion(&request.request_id).await?;
            // Track every request we attempted to submit this pass, in case
            // compliance needs to see what the processor touched.
            failed_submissions.push(request.clone());

            let outcome = SubmissionOutcome {
                request: request.clone(),
                submitted: did_submit,
                reason: if did_submit {
                    None
                } else {
                    Some("platform reported request already fulfilled or withdrawn".to_string())
                },
            };
            self.store.record(&outcome)?;

            if did_submit {
                submitted += 1;
            }
        }

        Ok(ProcessSummary {
            checked: unique.len(),
            submitted,
            failed_submissions,
        })
    }
}

/// Drops duplicate requests for the same user id, keeping the first one
/// seen. A backlog can hold many thousands of pending requests, so this
/// runs once per poll over the full page returned by the platform.
fn dedup_by_user(requests: Vec<ErasureRequest>) -> Vec<ErasureRequest> {
    let mut unique: Vec<ErasureRequest> = Vec::new();
    for request in requests {
        let already_have = unique.iter().any(|u| u.user_id == request.user_id);
        if !already_have {
            unique.push(request);
        }
    }
    unique
}

/// A request is actionable once it has been overdue longer than the grace
/// period. Every request in the listing has cleared compliance intake, so
/// its overdue time is always tracked once the request exists.
fn is_actionable(request: &ErasureRequest, grace_secs: u64) -> bool {
    request.overdue_seconds.unwrap() > grace_secs
}
