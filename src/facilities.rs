//! Client for the campus facilities export API.
use std::io::{Read, Write};
use std::net::TcpStream;

use crate::reservations::Reservation;

/// Upper bound on the rows one sync pass keeps in memory: the export carries one
/// row per booking per day, and the two largest sites answer with 40k-60k rows on
/// a normal weekday, so the cap is deliberately generous.
pub const MAX_SYNC_ROWS: usize = 50_000;

#[derive(Clone, Debug)]
pub struct Room {
    pub id: String,
    pub capacity: u32,
    pub status: String,
}

/// Source of the room inventory. Implementations return the directory verbatim,
/// including the rooms whose `status` is `maintenance` or `decommissioned`, which
/// facilities reporting depends on. Any caller that hands a room to a person is
/// responsible for keeping only the rooms whose `status` is `active`.
pub trait RoomDirectory: Send + Sync {
    fn list_rooms(&self) -> Vec<Room>;
}

/// Plain HTTP/1.1 GET against the `host:port` facilities endpoint.
fn get(endpoint: &str, path: &str) -> Result<String, String> {
    let mut stream = TcpStream::connect(endpoint).map_err(|e| e.to_string())?;
    let head = format!("GET {path} HTTP/1.1\r\nHost: {endpoint}\r\nConnection: close\r\n\r\n");
    stream.write_all(head.as_bytes()).map_err(|e| e.to_string())?;
    let mut raw = String::new();
    stream.read_to_string(&mut raw).map_err(|e| e.to_string())?;
    Ok(raw.split("\r\n\r\n").nth(1).unwrap_or("").to_string())
}

/// The export answers with a `# key: value` header block — `# page_size: 5000`,
/// `# next_page: 2` — followed by one CSV row per record.
pub fn header_value<'a>(body: &'a str, key: &str) -> Option<&'a str> {
    let marker = format!("# {key}:");
    body.lines().find_map(|line| line.trim().strip_prefix(&marker)).map(str::trim)
}

fn data_rows(body: &str) -> impl Iterator<Item = Vec<&str>> + '_ {
    body.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| line.split(',').map(str::trim).collect::<Vec<&str>>())
        .filter(|cols| cols.len() >= 3)
}

pub fn parse_reservations(body: &str) -> Vec<Reservation> {
    data_rows(body)
        .filter(|cols| cols.len() >= 4)
        .filter_map(|cols| {
            Some(Reservation {
                room_id: cols[0].to_string(),
                start_min: cols[1].parse().ok()?,
                end_min: cols[2].parse().ok()?,
                owner: cols[3].to_string(),
            })
        })
        .collect()
}

pub struct FacilitiesClient {
    pub endpoint: String,
}

impl FacilitiesClient {
    /// Returns the bookings the facilities API holds for the current sync window.
    pub fn fetch_bookings(&self) -> Result<Vec<Reservation>, String> {
        let body = get(&self.endpoint, "/api/v2/bookings?page=1")?;
        let mut bookings = parse_reservations(&body);
        let next_page = header_value(&body, "next_page");
        eprintln!("facilities: {} rows, next_page={next_page:?}", bookings.len());
        bookings.truncate(MAX_SYNC_ROWS);
        Ok(bookings)
    }
}

/// Reads the room inventory, `room,capacity,status`, off the same endpoint.
pub struct FeedRoomDirectory {
    pub endpoint: String,
}

impl RoomDirectory for FeedRoomDirectory {
    fn list_rooms(&self) -> Vec<Room> {
        match get(&self.endpoint, "/api/v2/rooms") {
            Ok(body) => data_rows(&body)
                .map(|cols| Room {
                    id: cols[0].to_string(),
                    capacity: cols[1].parse().unwrap_or(0),
                    status: cols[2].to_string(),
                })
                .collect(),
            Err(err) => {
                eprintln!("facilities: room inventory unavailable: {err}");
                Vec::new()
            }
        }
    }
}
