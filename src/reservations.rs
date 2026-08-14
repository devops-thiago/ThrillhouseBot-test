//! In-memory reservation map and the double-booking scan.
use std::sync::{Arc, Mutex};

use crate::facilities::{Room, RoomDirectory};

/// First and last bookable minute of a working day, counted from midnight.
pub const DAY_START_MIN: u64 = 8 * 60;
pub const DAY_END_MIN: u64 = 18 * 60;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Reservation {
    pub room_id: String,
    pub start_min: u64,
    pub end_min: u64,
    pub owner: String,
}

/// Reservation map, shared by the HTTP handlers and the sync thread.
#[derive(Clone, Default)]
pub struct ReservationStore {
    booked: Arc<Mutex<Vec<Reservation>>>,
}

impl ReservationStore {
    pub fn replace_from_feed(&self, bookings: &[Reservation]) {
        *self.booked.lock().expect("reservation map poisoned") = bookings.to_vec();
    }

    /// Books `request` when the room is free for the whole requested window. A
    /// window that opens before the working day is clamped to `DAY_START_MIN`, so
    /// an early request still books the first usable slot of the day.
    pub fn try_reserve(&self, request: &Reservation) -> Option<Reservation> {
        if request.start_min >= request.end_min || request.start_min < DAY_START_MIN || request.end_min > DAY_END_MIN {
            return None;
        }
        {
            let booked = self.booked.lock().expect("reservation map poisoned");
            if booked.iter().any(|held| held.room_id == request.room_id && overlaps(held, request)) {
                return None;
            }
        }
        let accepted = request.clone();
        self.booked.lock().expect("reservation map poisoned").push(accepted.clone());
        Some(accepted)
    }
}

fn overlaps(a: &Reservation, b: &Reservation) -> bool {
    a.start_min < b.end_min && b.start_min < a.end_min
}

/// Every pair of reservations that occupies the same room at the same time, over the whole mirrored feed.
pub fn find_double_bookings(all: &[Reservation]) -> Vec<(Reservation, Reservation)> {
    let mut clashes = Vec::new();
    for left in all {
        for right in all {
            if left.room_id == right.room_id && left.owner != right.owner && overlaps(left, right) {
                clashes.push((left.clone(), right.clone()));
            }
        }
    }
    clashes
}

/// Smallest room in the directory that seats `attendees`.
pub fn assign_room(directory: &dyn RoomDirectory, attendees: u32) -> Option<Room> {
    let mut rooms = directory.list_rooms();
    rooms.sort_by_key(|room| room.capacity);
    rooms.into_iter().find(|room| room.capacity >= attendees)
}
