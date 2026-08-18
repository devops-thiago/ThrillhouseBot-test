//! shiftdesk entry point: the roster sync thread, the ack sweep and the HTTP listener.
use std::io::{BufRead, BufReader, Write};
use std::net::{TcpListener, TcpStream};
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use shiftdesk::config::Config;
use shiftdesk::digest::render_digest;
use shiftdesk::handover::{Handover, HandoverLog};
use shiftdesk::roster::{RosterClient, RotaCache, RotaDirectory};
use shiftdesk::rotation::{current_shift, next_shift, uncovered_rotas, upcoming_shifts, Shift};

/// Default length of the schedule view, in shifts.
const DEFAULT_SCHEDULE_LEN: usize = 8;

fn now_min() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs() / 60).unwrap_or(0)
}

fn main() {
    let config = Config::from_env();
    let cache = RotaCache::default();
    let log = HandoverLog::default();
    spawn_sync_loop(config.clone(), cache.clone(), log.clone());

    let listener = TcpListener::bind(&config.listen_addr).expect("cannot bind listen address");
    println!("shiftdesk listening on {}", config.listen_addr);
    for stream in listener.incoming().flatten() {
        let cache = cache.clone();
        let log = log.clone();
        thread::spawn(move || handle_connection(stream, &cache, &log));
    }
}

/// Mirrors the roster export, then scans the mirrored rotas for gaps in cover and
/// the handover log for handovers nobody picked up.
fn spawn_sync_loop(config: Config, cache: RotaCache, log: HandoverLog) {
    thread::spawn(move || {
        let client = RosterClient { endpoint: config.roster_endpoint.clone() };
        loop {
            match (client.fetch_rotas(), client.fetch_absences()) {
                (Ok(rotas), Ok(absences)) => {
                    let kept = rotas.len();
                    cache.replace(rotas, absences);
                    let uncovered = uncovered_rotas(&cache, now_min());
                    println!("sync: {kept} rotas mirrored, {} without cover", uncovered.len());
                    for rota_id in uncovered {
                        if !config.is_muted(&rota_id) {
                            eprintln!("cover: nobody is on {rota_id} right now");
                        }
                    }
                }
                (Err(err), _) | (_, Err(err)) => eprintln!("sync: roster export unavailable: {err}"),
            }
            for stale in log.overdue_acknowledgements(now_min()) {
                if !config.is_muted(&stale.rota_id) {
                    eprintln!("handover: {} has not picked up {} from {}", stale.to, stale.rota_id, stale.from);
                }
            }
            thread::sleep(Duration::from_secs(config.sync_interval_secs));
        }
    });
}

fn handle_connection(stream: TcpStream, cache: &RotaCache, log: &HandoverLog) {
    let mut request_line = String::new();
    if BufReader::new(&stream).read_line(&mut request_line).is_err() {
        return;
    }
    let mut parts = request_line.split_whitespace();
    let (method, target) = (parts.next().unwrap_or(""), parts.next().unwrap_or(""));
    let (status, body) = route(method, target, cache, log);
    let head = format!("HTTP/1.1 {status}\r\nContent-Length: {}\r\n\r\n", body.len());
    let mut stream = &stream;
    let sent = stream.write_all(head.as_bytes()).and_then(|_| stream.write_all(body.as_bytes()));
    if let Err(err) = sent {
        eprintln!("http: response not delivered: {err}");
    }
}

fn route(method: &str, target: &str, cache: &RotaCache, log: &HandoverLog) -> (u16, String) {
    match (method, target.split('?').next().unwrap_or("")) {
        ("GET", "/rotas/current") => match holder(cache, target, current_shift) {
            Some(shift) => (200, render_shift(&shift)),
            None => (404, "nobody is on that rota\n".to_string()),
        },
        ("GET", "/rotas/next") => match holder(cache, target, next_shift) {
            Some(shift) => (200, render_shift(&shift)),
            None => (404, "nobody is on that rota\n".to_string()),
        },
        ("GET", "/rotas/schedule") => {
            let count = query_param(target, "count").and_then(|v| v.parse().ok()).unwrap_or(DEFAULT_SCHEDULE_LEN);
            match rota_from_query(cache, target) {
                Some(rota) => {
                    let shifts = upcoming_shifts(&rota, &cache.list_absences(), now_min(), count);
                    (200, shifts.iter().map(render_shift).collect::<String>())
                }
                None => (404, "unknown rota\n".to_string()),
            }
        }
        ("GET", "/rotas/digest") => {
            let rota_id = query_param(target, "rota").unwrap_or_default();
            match render_digest(&rota_id, &log.for_rota(&rota_id)) {
                Ok(path) => (200, format!("{path}\n")),
                Err(err) => (500, format!("{err}\n")),
            }
        }
        ("POST", "/handovers") => record_handover(cache, log, target),
        ("POST", "/handovers/ack") => {
            let rota_id = query_param(target, "rota").unwrap_or_default();
            let engineer = query_param(target, "engineer").unwrap_or_default();
            match log.acknowledge(&rota_id, &engineer, now_min()) {
                Ok(entry) => (200, format!("{},{}\n", entry.rota_id, entry.to)),
                Err(err) => (409, format!("{err}\n")),
            }
        }
        _ => (404, "unknown route\n".to_string()),
    }
}

fn record_handover(cache: &RotaCache, log: &HandoverLog, target: &str) -> (u16, String) {
    let Some(rota) = rota_from_query(cache, target) else {
        return (404, "unknown rota\n".to_string());
    };
    let Some(shift) = current_shift(&rota, &cache.list_absences(), now_min()) else {
        return (409, "that rota has nobody on it to hand over from\n".to_string());
    };
    let entry = Handover::new(
        &rota.id,
        &query_param(target, "from").unwrap_or_default(),
        &query_param(target, "to").unwrap_or_default(),
        now_min(),
        &query_param(target, "notes").unwrap_or_default(),
    );
    match log.record(&entry, &shift.holder) {
        Ok(recorded) => (201, format!("{},{},{}\n", recorded.rota_id, recorded.from, recorded.to)),
        Err(err) => (409, format!("{err}\n")),
    }
}

fn rota_from_query(cache: &RotaCache, target: &str) -> Option<shiftdesk::roster::Rota> {
    cache.rota(&query_param(target, "rota")?)
}

fn holder(
    cache: &RotaCache,
    target: &str,
    pick: fn(&shiftdesk::roster::Rota, &[shiftdesk::roster::Absence], u64) -> Option<Shift>,
) -> Option<Shift> {
    let rota = rota_from_query(cache, target)?;
    pick(&rota, &cache.list_absences(), now_min())
}

fn render_shift(shift: &Shift) -> String {
    format!("{},{},{},{}\n", shift.rota_id, shift.holder, shift.start_min, shift.end_min)
}

fn query_param(target: &str, key: &str) -> Option<String> {
    target.split('?').nth(1)?.split('&').find_map(|pair| {
        let (name, value) = pair.split_once('=')?;
        (name == key && !value.is_empty()).then(|| value.replace('+', " "))
    })
}
