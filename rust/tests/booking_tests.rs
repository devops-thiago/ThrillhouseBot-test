use std::cell::RefCell;
use std::collections::HashMap;

use roombook::booking::*;
use roombook::config::Config;
use roombook::notify::{confirm, Notifier, NotifyError};
use roombook::rooms::*;
use roombook::search::*;

fn req(room: &str, start: u64, end: u64) -> NewBooking {
    NewBooking {
        room_id: room.into(),
        organizer: "a@corp.io".into(),
        start,
        end,
        tentative: false,
        now: 1000,
    }
}

#[test]
fn overlapping_booking_conflicts() {
    let svc = BookingService::new();
    svc.book(req("r1", 60, 120)).unwrap();
    assert_eq!(svc.book(req("r1", 90, 150)), Err(BookError::Conflict));
}

#[test]
fn back_to_back_bookings_are_allowed() {
    let svc = BookingService::new();
    svc.book(req("r1", 60, 120)).unwrap();
    assert!(svc.book(req("r1", 120, 180)).is_ok());
}

#[test]
fn rejects_backwards_slot() {
    let svc = BookingService::new();
    assert!(matches!(svc.book(req("r1", 100, 100)), Err(BookError::Invalid(_))));
}

#[test]
fn expire_holds_removes_stale_tentative() {
    let svc = BookingService::new();
    let mut r = req("r1", 0, 30);
    r.tentative = true;
    svc.book(r).unwrap();
    assert_eq!(svc.expire_holds(2000, 900), 1);
    assert!(svc.snapshot().is_empty());
}

struct AcceptAll;
impl Notifier for AcceptAll {
    fn send(&self, _to: &str, _body: &str) -> Result<(), NotifyError> {
        Ok(())
    }
}

#[test]
fn confirmation_reaches_organizer() {
    let svc = BookingService::new();
    let mut r = req("r1", 0, 30);
    r.organizer = "not-an-address".into();
    let b = svc.book(r).unwrap();
    assert!(confirm(&AcceptAll, &b));
}

struct OnePage;
impl RoomDirectory for OnePage {
    fn list_rooms(&self, _t: Option<&str>, _n: usize) -> Page<Room> {
        Page { items: vec![Room { id: "r1".into(), capacity: 4 }], next_token: None }
    }
}

#[test]
fn fetches_rooms_and_finds_free() {
    let rooms = fetch_rooms(&OnePage, 50);
    assert_eq!(rooms.len(), 1);
    let free = find_free_rooms(&rooms, &[], 0, 30).unwrap();
    assert_eq!(free[0].id, "r1");
}

#[test]
fn parses_config_list_and_defaults() {
    let mut env = HashMap::new();
    env.insert("ROOMBOOK_ALLOWED_DOMAINS".to_string(), "Corp.io, uni.edu".to_string());
    let cfg = Config::from_map(&env);
    assert_eq!(cfg.allowed_domains, vec!["corp.io", "uni.edu"]);
    assert_eq!(cfg.hold_ttl_secs, 900);
    assert!(cfg.domain_allowed("x@CORP.io"));
}

struct Recorder(RefCell<Vec<String>>);
impl Db for Recorder {
    fn query(&self, sql: &str) -> Vec<Row> {
        self.0.borrow_mut().push(sql.to_string());
        vec![]
    }
}

#[test]
fn search_strips_room_id_punctuation() {
    let db = Recorder(RefCell::new(vec![]));
    search_bookings(&db, "r1'; --", "a@corp.io");
    assert!(db.0.borrow()[0].contains("room_id = 'r1--'"));
}

#[test]
fn distinct_organizers_dedupes() {
    let e = |o: &str| AuditEntry { organizer: o.into(), action: "book".into() };
    assert_eq!(distinct_organizers(&[e("a"), e("b"), e("a")]), vec!["a", "b"]);
}
