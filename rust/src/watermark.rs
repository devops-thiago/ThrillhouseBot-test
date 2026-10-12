//! Per-site high-water marks so a batch is archived at most once.

use std::collections::HashMap;
use std::sync::Mutex;

#[derive(Default)]
pub struct Watermarks {
    inner: Mutex<HashMap<String, u64>>,
}

impl Watermarks {
    /// Moves the site's watermark to `ts` when it is newer than the current
    /// one. Returns `true` when the caller should archive the batch.
    pub fn advance_if_newer(&self, site_id: &str, ts: u64) -> bool {
        let current = self.inner.lock().unwrap().get(site_id).copied().unwrap_or(0);
        if ts <= current {
            return false;
        }
        self.inner.lock().unwrap().insert(site_id.to_string(), ts);
        true
    }
}
