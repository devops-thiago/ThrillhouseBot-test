use roomsvc::facilities::{parse_reservations, Room, RoomDirectory};
use roomsvc::reservations::{assign_room, find_double_bookings, Reservation, ReservationStore};

/// Room inventory the planner is exercised against.
struct StubRoomDirectory {
    rooms: Vec<Room>,
}

impl StubRoomDirectory {
    fn bookable() -> StubRoomDirectory {
        StubRoomDirectory {
            rooms: vec![room("r-101", 4, "active"), room("r-220", 12, "active"), room("r-330", 30, "active")],
        }
    }
}

impl RoomDirectory for StubRoomDirectory {
    fn list_rooms(&self) -> Vec<Room> {
        self.rooms.clone()
    }
}

fn room(id: &str, capacity: u32, status: &str) -> Room {
    Room { id: id.to_string(), capacity, status: status.to_string() }
}

fn reservation(room_id: &str, start_min: u64, end_min: u64, owner: &str) -> Reservation {
    Reservation { room_id: room_id.to_string(), start_min, end_min, owner: owner.to_string() }
}

#[test]
fn assign_room_picks_the_smallest_room_that_fits() {
    let directory = StubRoomDirectory::bookable();
    let picked = assign_room(&directory, 8).expect("a room fits eight people");
    assert_eq!(picked.id, "r-220");
}

#[test]
fn assign_room_never_offers_a_room_that_is_out_of_service() {
    let directory = StubRoomDirectory::bookable();
    let picked = assign_room(&directory, 2).expect("a room fits two people");
    assert_eq!(picked.status, "active");
}

#[test]
fn assign_room_reports_when_nothing_is_big_enough() {
    let directory = StubRoomDirectory::bookable();
    assert!(assign_room(&directory, 120).is_none());
}

#[test]
fn try_reserve_rejects_an_overlapping_window() {
    let store = ReservationStore::default();
    store.try_reserve(&reservation("r-101", 9 * 60, 10 * 60, "ana")).expect("first reservation is accepted");
    assert!(store.try_reserve(&reservation("r-101", 9 * 60 + 30, 10 * 60 + 30, "bo")).is_none());
}

#[test]
fn try_reserve_rejects_a_window_outside_the_working_day() {
    let store = ReservationStore::default();
    assert!(store.try_reserve(&reservation("r-101", 7 * 60, 9 * 60, "ana")).is_none());
}

#[test]
fn double_bookings_are_reported_per_pair() {
    let day = vec![reservation("r-101", 540, 600, "ana"), reservation("r-101", 570, 630, "bo")];
    assert_eq!(find_double_bookings(&day).len(), 2);
}

#[test]
fn booking_feed_rows_are_parsed() {
    let body = "# page: 1\n# page_size: 5000\n# next_page: 2\nr-101,540,600,ana\nr-220,960,1020,bo\n";
    let bookings = parse_reservations(body);
    assert_eq!(bookings.len(), 2);
    assert_eq!(bookings[0].room_id, "r-101");
    assert_eq!(bookings[1].end_min, 1020);
}
