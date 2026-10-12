//! Shared ingest path used by the HTTP handler and the backfill scheduler.

use std::path::PathBuf;

use crate::archive::{archive_readings, is_valid_site_id};
use crate::telemetry::{order_for_archive, parse_line, summarize, utilisation, Reading, StaticRatings};
use crate::watermark::Watermarks;

pub struct AppState {
    pub archive_dir: PathBuf,
    pub watermarks: Watermarks,
    pub ratings: StaticRatings,
}

/// Parses a request body of telemetry lines and runs it through the pipeline.
pub fn ingest_body(state: &AppState, site_id: &str, body: &str) -> (u16, String) {
    if !is_valid_site_id(site_id) {
        return (400, "invalid site id".into());
    }
    let mut readings = Vec::new();
    for line in body.lines().filter(|l| !l.trim().is_empty()) {
        match parse_line(site_id, line) {
            Ok(r) => readings.push(r),
            Err(e) => return (400, format!("bad line {line:?}: {e:?}")),
        }
    }
    process_batch(state, site_id, readings)
}

pub fn process_batch(state: &AppState, site_id: &str, mut readings: Vec<Reading>) -> (u16, String) {
    let newest = match readings.iter().map(|r| r.timestamp).max() {
        Some(ts) => ts,
        None => return (204, String::new()),
    };
    if !state.watermarks.advance_if_newer(site_id, newest) {
        return (409, "batch is not newer than the site watermark".into());
    }

    order_for_archive(&mut readings);
    let util = utilisation(&readings, &state.ratings);
    let summary = summarize(readings);
    if !summary.faulted_readings.is_empty() {
        eprintln!("ALERT site {site_id}: {} faulted readings", summary.faulted_readings.len());
    }

    match archive_readings(&state.archive_dir, site_id, &summary.readings) {
        Ok(rows) => {
            let faults = summary.faulted_readings.len();
            (202, format!("archived={rows} total_watts={:.0} faults={faults} rated={}", summary.total_watts, util.len()))
        }
        Err(e) => (500, format!("archive failed: {e}")),
    }
}
