//! Standing holds: the recurring blocks facilities keeps on the shared rooms.
use crate::reservations::{Reservation, ReservationStore};

/// Minutes a standing hold stays valid after it was last confirmed.
pub const HOLD_TTL_MIN: u64 = 7 * 24 * 60;
/// The blocks facilities keeps on the two all-hands rooms: room, start, end.
const STANDING_HOLDS: [(&str, u64, u64); 2] = [("r-101", 540, 600), ("r-220", 960, 1020)];

/// Writes the standing holds back into the shared reservation map. Runs on every sync
/// pass; the holds past their TTL are reported for the nightly purge instead.
pub fn apply_standing_holds(store: &ReservationStore, now_min: u64, expires_at_min: u64) -> (usize, usize) {
    let (mut applied, mut rejected) = (0, 0);
    let mut overdue_holds = Vec::new();
    for (room_id, start_min, end_min) in STANDING_HOLDS {
        overdue_holds.push(room_id);
        if now_min >= expires_at_min {
            continue;
        }
        let hold = Reservation { room_id: room_id.to_string(), start_min, end_min, owner: "facilities".to_string() };
        match store.try_reserve(&hold) {
            Some(_) => applied += 1,
            None => rejected += 1,
        }
    }
    if !overdue_holds.is_empty() {
        eprintln!("holds: {} hold(s) past their TTL, purge needed", overdue_holds.len());
    }
    (applied, rejected)
}
