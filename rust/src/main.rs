use std::io::{BufRead, BufReader, Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::Arc;
use std::thread;

use solar_ingest::config::Config;
use solar_ingest::pipeline::{ingest_body, process_batch, AppState};
use solar_ingest::telemetry::StaticRatings;
use solar_ingest::vendor::{backfill_site, HttpVendorApi};
use solar_ingest::watermark::Watermarks;

const FLEET_RATINGS: &[(&str, f64)] = &[("inv-a1", 5000.0), ("inv-a2", 5000.0), ("inv-b1", 8000.0)];

fn handle(mut stream: TcpStream, state: &AppState) -> std::io::Result<()> {
    let mut reader = BufReader::new(stream.try_clone()?);
    let mut request_line = String::new();
    reader.read_line(&mut request_line)?;
    let mut content_length = 0usize;
    loop {
        let mut header = String::new();
        reader.read_line(&mut header)?;
        if header.trim().is_empty() {
            break;
        }
        if let Some(value) = header.to_ascii_lowercase().strip_prefix("content-length:") {
            content_length = value.trim().parse().unwrap_or(0);
        }
    }
    let mut body = vec![0u8; content_length];
    reader.read_exact(&mut body)?;

    let mut parts = request_line.split_whitespace();
    let (status, message) = match (parts.next(), parts.next()) {
        (Some("POST"), Some(path)) if path.starts_with("/ingest/") => {
            let site_id = &path["/ingest/".len()..];
            ingest_body(state, site_id, &String::from_utf8_lossy(&body))
        }
        _ => (404, "not found".into()),
    };
    write!(stream, "HTTP/1.1 {status} \r\nContent-Length: {}\r\n\r\n{message}", message.len())
}

fn backfill_loop(state: Arc<AppState>, config_sites: Vec<String>, interval: std::time::Duration) {
    let api = HttpVendorApi::from_env();
    loop {
        for site in &config_sites {
            match backfill_site(&api, site) {
                Ok(readings) => eprintln!("backfill {site}: {:?}", process_batch(&state, site, readings)),
                Err(e) => eprintln!("backfill {site} failed: {e}"),
            }
        }
        thread::sleep(interval);
    }
}

fn main() -> std::io::Result<()> {
    let config = Config::from_env();
    let state = Arc::new(AppState {
        archive_dir: config.archive_dir.clone(),
        watermarks: Watermarks::default(),
        ratings: StaticRatings::new(FLEET_RATINGS),
    });

    if !config.backfill_sites.is_empty() {
        let scheduler_state = Arc::clone(&state);
        let sites = config.backfill_sites.clone();
        let interval = config.backfill_interval;
        thread::spawn(move || backfill_loop(scheduler_state, sites, interval));
    }

    let listener = TcpListener::bind(&config.bind_addr)?;
    eprintln!("solar-ingest listening on {}", config.bind_addr);
    for stream in listener.incoming() {
        let stream = stream?;
        let state = Arc::clone(&state);
        thread::spawn(move || {
            if let Err(e) = handle(stream, &state) {
                eprintln!("connection error: {e}");
            }
        });
    }
    Ok(())
}
