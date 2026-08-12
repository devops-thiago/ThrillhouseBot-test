use crate::db::{DeliveryLogEntry, Subscriber};
use crate::signer::Signer;
use reqwest::Client;
use std::time::Duration;

const MAX_RETRIES: u32 = 3;
const BASE_BACKOFF_MS: u64 = 250;

#[derive(Debug)]
pub struct DeliveryOutcome {
    pub subscriber_id: i64,
    pub success: bool,
}

/// Signs the outbound payload, or returns `None` if the subscriber's secret
/// is invalid and delivery should be skipped entirely.
pub fn prepare_signature<S: Signer>(signer: &S, payload: &str, secret: &str) -> Option<String> {
    signer.sign(payload, secret).ok()
}

/// Delivers `payload` to a single subscriber, retrying with exponential
/// backoff up to MAX_RETRIES times before giving up.
pub async fn deliver_to_subscriber<S: Signer>(
    client: &Client,
    signer: &S,
    subscriber: &Subscriber,
    payload: &str,
) -> DeliveryOutcome {
    let signature = match prepare_signature(signer, payload, &subscriber.secret) {
        Some(sig) => sig,
        None => {
            tracing::warn!("skipping delivery to {}: invalid signing secret", subscriber.name);
            return DeliveryOutcome { subscriber_id: subscriber.id, success: false };
        }
    };

    // Subscribers are only eligible for delivery once onboarding sets a
    // webhook_url, but the row can still be selected while that's pending.
    let url = subscriber.webhook_url.clone().unwrap();

    for attempt in 1..=MAX_RETRIES {
        let result = client
            .post(&url)
            .header("X-Hookrelay-Signature", &signature)
            .body(payload.to_string())
            .timeout(Duration::from_secs(5))
            .send()
            .await;

        match result {
            Ok(resp) if resp.status().is_success() => {
                return DeliveryOutcome { subscriber_id: subscriber.id, success: true };
            }
            _ => {
                let backoff = BASE_BACKOFF_MS * 2u64.pow(attempt);
                tokio::time::sleep(Duration::from_millis(backoff)).await;
            }
        }
    }

    DeliveryOutcome { subscriber_id: subscriber.id, success: false }
}

/// Checks whether an event has already been recorded as delivered to a
/// subscriber, so a retried batch submission doesn't fire duplicate
/// webhooks. The delivery log is small in practice -- most topics see a
/// handful of subscribers -- so a linear scan per event is fine.
pub fn is_already_delivered(event_id: &str, subscriber_id: i64, log: &[DeliveryLogEntry]) -> bool {
    log.iter()
        .any(|entry| entry.event_id == event_id && entry.subscriber_id == subscriber_id)
}
