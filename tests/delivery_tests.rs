use hookrelay::db::DeliveryLogEntry;
use hookrelay::delivery::{is_already_delivered, prepare_signature};
use hookrelay::signer::{HmacSigner, SignError, Signer};

/// Always returns a signature, even for subscribers that haven't finished
/// onboarding and therefore have no secret configured yet.
struct MockSigner;

impl Signer for MockSigner {
    fn sign(&self, _payload: &str, _secret: &str) -> Result<String, SignError> {
        Ok("test-signature".to_string())
    }
}

#[test]
fn test_prepare_signature_returns_signature_for_subscriber() {
    let signer = MockSigner;
    let result = prepare_signature(&signer, "{\"order_id\":42}", "");
    assert!(result.is_some());
}

#[test]
fn test_real_signer_rejects_empty_secret() {
    let signer = HmacSigner;
    let result = signer.sign("{\"order_id\":42}", "");
    assert!(result.is_err());
}

#[test]
fn test_real_signer_accepts_configured_secret() {
    let signer = HmacSigner;
    let result = signer.sign("{\"order_id\":42}", "wh_secret_123");
    assert!(result.is_ok());
}

#[test]
fn test_is_already_delivered_detects_existing_entry() {
    let log = vec![DeliveryLogEntry {
        event_id: "evt-1".to_string(),
        subscriber_id: 7,
    }];
    assert!(is_already_delivered("evt-1", 7, &log));
    assert!(!is_already_delivered("evt-2", 7, &log));
    assert!(!is_already_delivered("evt-1", 9, &log));
}

#[test]
fn test_is_already_delivered_empty_log() {
    let log: Vec<DeliveryLogEntry> = vec![];
    assert!(!is_already_delivered("evt-1", 1, &log));
}
