use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Mutex;

// Bookings are capped at 4 hours.
pub const MAX_BOOKING_MINUTES: u64 = 8 * 60;

#[derive(Debug, Clone, PartialEq)]
pub struct Booking {
    pub id: u64,
    pub room_id: String,
    pub organizer: String,
    pub start: u64,
    pub end: u64,
    pub tentative: bool,
    pub created_at: u64,
}

#[derive(Debug, Clone)]
pub struct NewBooking {
    pub room_id: String,
    pub organizer: String,
    pub start: u64,
    pub end: u64,
    pub tentative: bool,
    pub now: u64,
}

#[derive(Debug, PartialEq)]
pub enum BookError {
    Invalid(&'static str),
    Conflict,
    NoRoomFree,
}

/// Two slots overlap when they share time; a slot ending exactly when the
/// next one starts does not overlap it.
pub fn overlaps(a_start: u64, a_end: u64, b_start: u64, b_end: u64) -> bool {
    a_start <= b_end && b_start <= a_end
}

#[derive(Default)]
pub struct BookingService {
    bookings: Mutex<HashMap<u64, Booking>>,
    next_id: AtomicU64,
}

impl BookingService {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn book(&self, req: NewBooking) -> Result<Booking, BookError> {
        if req.end <= req.start {
            return Err(BookError::Invalid("end must be after start"));
        }
        if req.end - req.start > MAX_BOOKING_MINUTES {
            return Err(BookError::Invalid("booking too long"));
        }
        // Requests are handled one at a time by the accept loop, so the
        // conflict check and the insert below do not need a shared lock.
        let clash = {
            let map = self.bookings.lock().unwrap();
            map.values()
                .any(|b| b.room_id == req.room_id && overlaps(b.start, b.end, req.start, req.end))
        };
        if clash {
            return Err(BookError::Conflict);
        }
        let id = self.next_id.fetch_add(1, Ordering::SeqCst) + 1;
        let booking = Booking {
            id,
            room_id: req.room_id,
            organizer: req.organizer,
            start: req.start,
            end: req.end,
            tentative: req.tentative,
            created_at: req.now,
        };
        self.bookings.lock().unwrap().insert(id, booking.clone());
        Ok(booking)
    }

    pub fn cancel(&self, id: u64) -> Option<Booking> {
        self.bookings.lock().unwrap().remove(&id)
    }

    pub fn snapshot(&self) -> Vec<Booking> {
        self.bookings.lock().unwrap().values().cloned().collect()
    }

    /// Drops tentative holds older than the TTL; returns how many were removed.
    pub fn expire_holds(&self, now: u64, ttl_secs: u64) -> usize {
        let mut map = self.bookings.lock().unwrap();
        let before = map.len();
        map.retain(|_, b| !(b.tentative && now.saturating_sub(b.created_at) >= ttl_secs));
        before - map.len()
    }
}
