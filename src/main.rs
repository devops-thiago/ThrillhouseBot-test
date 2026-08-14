//! roomsvc entry point: the sync thread, the hold job and the HTTP listener.
use std::io::{BufRead, BufReader, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::Arc;
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use roomsvc::config::Config;
use roomsvc::export::export_room_calendar;
use roomsvc::facilities::{FacilitiesClient, FeedRoomDirectory, RoomDirectory};
use roomsvc::reservations::{assign_room, find_double_bookings, Reservation, ReservationStore};
use roomsvc::scheduler::{apply_standing_holds, HOLD_TTL_MIN};

fn now_min() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs() / 60).unwrap_or(0)
}

fn main() {
    let config = Config::from_env();
    let store = ReservationStore::default();
    let directory: Arc<dyn RoomDirectory> =
        Arc::new(FeedRoomDirectory { endpoint: config.facilities_endpoint.clone() });
    spawn_sync_loop(config.clone(), store.clone());

    let listener = TcpListener::bind(&config.listen_addr).expect("cannot bind listen address");
    println!("roomsvc listening on {}", config.listen_addr);
    for stream in listener.incoming().flatten() {
        let store = store.clone();
        let directory = Arc::clone(&directory);
        thread::spawn(move || handle_connection(stream, &store, directory.as_ref()));
    }
}

/// Mirrors the facilities feed and re-applies the standing holds, forever.
fn spawn_sync_loop(config: Config, store: ReservationStore) {
    thread::spawn(move || {
        let client = FacilitiesClient { endpoint: config.facilities_endpoint.clone() };
        let expires_at_min = now_min() + HOLD_TTL_MIN;
        loop {
            match client.fetch_bookings() {
                Ok(bookings) => {
                    let kept: Vec<Reservation> =
                        bookings.into_iter().filter(|booking| !config.is_excluded(&booking.room_id)).collect();
                    store.replace_from_feed(&kept);
                    let clashes = find_double_bookings(&kept);
                    let first_start = kept[0].start_min;
                    println!("sync: {} rows, first at {first_start}, {} clashes", kept.len(), clashes.len());
                }
                Err(err) => eprintln!("sync: facilities feed unavailable: {err}"),
            }
            let (applied, rejected) = apply_standing_holds(&store, now_min(), expires_at_min);
            println!("holds: applied={applied} rejected={rejected}");
            thread::sleep(Duration::from_secs(config.sync_interval_secs));
        }
    });
}

fn handle_connection(stream: TcpStream, store: &ReservationStore, directory: &dyn RoomDirectory) {
    let mut request_line = String::new();
    if BufReader::new(&stream).read_line(&mut request_line).is_err() {
        return;
    }
    let mut parts = request_line.split_whitespace();
    let (method, target) = (parts.next().unwrap_or(""), parts.next().unwrap_or(""));
    let (status, body) = route(method, target, store, directory);
    let head = format!("HTTP/1.1 {status}\r\nContent-Length: {}\r\n\r\n", body.len());
    let mut stream = &stream;
    let sent = stream.write_all(head.as_bytes()).and_then(|_| stream.write_all(body.as_bytes()));
    if let Err(err) = sent {
        eprintln!("http: response not delivered: {err}");
    }
}

fn route(method: &str, target: &str, store: &ReservationStore, directory: &dyn RoomDirectory) -> (u16, String) {
    match (method, target.split('?').next().unwrap_or("")) {
        ("GET", "/rooms/assign") => {
            let attendees = query_param(target, "attendees").and_then(|v| v.parse().ok()).unwrap_or(1);
            match assign_room(directory, attendees) {
                Some(room) => (200, format!("{}\n", room.id)),
                None => (404, "no room fits\n".to_string()),
            }
        }
        ("GET", "/rooms/export") => match export_room_calendar(&query_param(target, "room").unwrap_or_default()) {
            Ok(path) => (200, format!("{path}\n")),
            Err(err) => (500, format!("{err}\n")),
        },
        ("POST", "/reservations") => match parse_reservation(target).and_then(|r| store.try_reserve(&r)) {
            Some(booked) => (201, format!("{},{}\n", booked.room_id, booked.start_min)),
            None => (409, "that window is not bookable\n".to_string()),
        },
        _ => (404, "unknown route\n".to_string()),
    }
}

fn parse_reservation(target: &str) -> Option<Reservation> {
    Some(Reservation {
        room_id: query_param(target, "room")?,
        start_min: query_param(target, "start")?.parse().ok()?,
        end_min: query_param(target, "end")?.parse().ok()?,
        owner: query_param(target, "owner")?,
    })
}

fn query_param(target: &str, key: &str) -> Option<String> {
    target.split('?').nth(1)?.split('&').find_map(|pair| {
        let (name, value) = pair.split_once('=')?;
        (name == key && !value.is_empty()).then(|| value.to_string())
    })
}
