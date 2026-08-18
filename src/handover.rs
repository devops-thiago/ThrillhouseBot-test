//! The handover log: who passed a rota to whom, and whether it was picked up.
use std::sync::{Arc, Mutex};

/// How long the incoming holder has to acknowledge a handover before the sweep
/// reports it as unanswered.
pub const ACK_GRACE_MIN: u64 = 30;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Handover {
    pub rota_id: String,
    pub from: String,
    pub to: String,
    pub at_min: u64,
    pub notes: String,
    pub acknowledged: bool,
}

impl Handover {
    pub fn new(rota_id: &str, from: &str, to: &str, at_min: u64, notes: &str) -> Handover {
        Handover {
            rota_id: rota_id.to_string(),
            from: from.to_string(),
            to: to.to_string(),
            at_min,
            notes: notes.to_string(),
            acknowledged: false,
        }
    }
}

/// Append-only log of handovers, shared by the HTTP handlers and the ack sweep.
#[derive(Clone, Default)]
pub struct HandoverLog {
    entries: Arc<Mutex<Vec<Handover>>>,
}

impl HandoverLog {
    /// Records a handover of `entry.rota_id`. The outgoing name has to be the one
    /// holding the rota at the time — an engineer cannot hand over a shift that is
    /// not theirs — and a rota cannot be handed to whoever already holds it. A
    /// repeat of a handover that is still waiting to be acknowledged is rejected
    /// too, so a retrying client does not fill the log with copies.
    pub fn record(&self, entry: &Handover, current_holder: &str) -> Result<Handover, String> {
        if entry.from != current_holder {
            return Err(format!("{} does not hold {}, {current_holder} does", entry.from, entry.rota_id));
        }
        if entry.to == entry.from {
            return Err("a rota cannot be handed to its current holder".to_string());
        }
        let mut entries = self.entries.lock().expect("handover log poisoned");
        let open_duplicate = entries
            .iter()
            .rev()
            .find(|held| held.rota_id == entry.rota_id)
            .is_some_and(|held| !held.acknowledged && held.to == entry.to);
        if open_duplicate {
            return Err(format!("{} has an unacknowledged handover of {} already", entry.to, entry.rota_id));
        }
        entries.push(entry.clone());
        Ok(entry.clone())
    }

    /// Marks the open handover of `rota_id` as picked up by `engineer`.
    pub fn acknowledge(&self, rota_id: &str, engineer: &str, at_min: u64) -> Result<Handover, String> {
        let mut entries = self.entries.lock().expect("handover log poisoned");
        let open = entries
            .iter_mut()
            .rev()
            .find(|held| held.rota_id == rota_id && !held.acknowledged)
            .ok_or_else(|| format!("{rota_id} has no handover waiting to be acknowledged"))?;
        if open.to != engineer {
            return Err(format!("{rota_id} was handed to {}, not to {engineer}", open.to));
        }
        open.acknowledged = true;
        open.at_min = at_min;
        Ok(open.clone())
    }

    pub fn latest(&self, rota_id: &str) -> Option<Handover> {
        let entries = self.entries.lock().expect("handover log poisoned");
        entries.iter().rev().find(|held| held.rota_id == rota_id).cloned()
    }

    pub fn for_rota(&self, rota_id: &str) -> Vec<Handover> {
        let entries = self.entries.lock().expect("handover log poisoned");
        entries.iter().filter(|held| held.rota_id == rota_id).cloned().collect()
    }

    /// Handovers still unacknowledged once the grace period is up. The sweep runs
    /// on every sync pass and pages the outgoing holder, who stays responsible for
    /// the rota until someone picks it up.
    pub fn overdue_acknowledgements(&self, now_min: u64) -> Vec<Handover> {
        let entries = self.entries.lock().expect("handover log poisoned");
        entries
            .iter()
            .filter(|held| !held.acknowledged && now_min.saturating_sub(held.at_min) >= ACK_GRACE_MIN)
            .cloned()
            .collect()
    }
}
