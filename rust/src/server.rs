use std::io::{Read, Write};
use std::net::TcpListener;
use std::sync::Arc;
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use crate::booking::{BookError, BookingService, NewBooking};
use crate::config::Config;
use crate::notify::{confirm, Notifier};

pub struct App {
    pub svc: BookingService,
    pub cfg: Config,
    pub notifier: Box<dyn Notifier + Send + Sync>,
}

fn now_secs() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

fn field(body: &str, key: &str) -> Option<String> {
    body.split('&').find_map(|kv| kv.strip_prefix(&format!("{key}=")).map(String::from))
}

pub fn handle_request(app: &App, method: &str, path: &str, body: &str) -> (u16, String) {
    match (method, path) {
        ("POST", "/bookings") => {
            let organizer = field(body, "organizer").unwrap_or_default();
            if !app.cfg.domain_allowed(&organizer) {
                return (403, "domain not allowed".into());
            }
            let num = |k: &str| field(body, k).and_then(|v| v.parse::<u64>().ok());
            let (Some(start), Some(end)) = (num("start"), num("end")) else {
                return (400, "bad times".into());
            };
            let req = NewBooking {
                room_id: field(body, "room").unwrap_or_default(),
                organizer,
                start,
                end,
                tentative: field(body, "tentative").as_deref() == Some("1"),
                now: now_secs(),
            };
            match app.svc.book(req) {
                Ok(b) => {
                    confirm(app.notifier.as_ref(), &b);
                    (201, format!("{}", b.id))
                }
                Err(BookError::Conflict) => (409, "room taken".into()),
                Err(_) => (400, "invalid booking".into()),
            }
        }
        _ => (404, "not found".into()),
    }
}

/// Scheduled task: expire stale tentative holds every minute.
pub fn spawn_hold_reaper(app: Arc<App>) {
    thread::spawn(move || loop {
        thread::sleep(Duration::from_secs(60));
        app.svc.expire_holds(now_secs(), app.cfg.hold_ttl_secs);
    });
}

pub fn serve(listener: TcpListener, app: Arc<App>) {
    for stream in listener.incoming().flatten() {
        let app = Arc::clone(&app);
        thread::spawn(move || {
            let mut stream = stream;
            let mut buf = [0u8; 4096];
            let n = stream.read(&mut buf).unwrap_or(0);
            let text = String::from_utf8_lossy(&buf[..n]).to_string();
            let mut parts = text.split_whitespace();
            let (method, path) = (parts.next().unwrap_or(""), parts.next().unwrap_or(""));
            let body = text.split("\r\n\r\n").nth(1).unwrap_or("");
            let (code, msg) = handle_request(&app, method, path, body);
            let _ = write!(stream, "HTTP/1.1 {code} X\r\nContent-Length: {}\r\n\r\n{msg}", msg.len());
        });
    }
}
