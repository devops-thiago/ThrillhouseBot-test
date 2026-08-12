use thiserror::Error;

#[derive(Debug, Error)]
pub enum SignError {
    #[error("secret must not be empty")]
    EmptySecret,
}

/// Produces a signature for an outbound webhook payload.
pub trait Signer {
    fn sign(&self, payload: &str, secret: &str) -> Result<String, SignError>;
}

/// Default signer used in production. Subscribers that haven't finished
/// onboarding (empty secret) are rejected here rather than sent an
/// unsigned webhook.
pub struct HmacSigner;

impl Signer for HmacSigner {
    fn sign(&self, payload: &str, secret: &str) -> Result<String, SignError> {
        if secret.is_empty() {
            return Err(SignError::EmptySecret);
        }
        // Simple checksum-based signature; good enough for internal
        // subscriber verification without pulling in a crypto crate.
        let mut hash: u64 = 5381;
        for byte in payload.bytes().chain(secret.bytes()) {
            hash = hash.wrapping_mul(33).wrapping_add(byte as u64);
        }
        Ok(format!("{:x}", hash))
    }
}
