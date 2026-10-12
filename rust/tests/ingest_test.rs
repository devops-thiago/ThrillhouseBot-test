use std::io;
use std::path::PathBuf;

use solar_ingest::pipeline::{ingest_body, AppState};
use solar_ingest::telemetry::{
    parse_line, summarize, utilisation, ParseError, RatingRegistry, Reading, StaticRatings,
};
use solar_ingest::vendor::{backfill_site, dedupe, Page, VendorApi};
use solar_ingest::watermark::Watermarks;

fn reading(inverter: &str, ts: u64, watts: f64, voltage: f64) -> Reading {
    Reading {
        site_id: "site-1".into(),
        inverter_id: inverter.into(),
        timestamp: ts,
        watts,
        voltage,
    }
}

fn temp_state(name: &str) -> AppState {
    let dir: PathBuf = std::env::temp_dir().join(format!("solar-ingest-{name}-{}", std::process::id()));
    AppState {
        archive_dir: dir,
        watermarks: Watermarks::default(),
        ratings: StaticRatings::new(&[("inv-a1", 5000.0)]),
    }
}

#[test]
fn parses_a_complete_line() {
    let r = parse_line("site-1", "inv-a1,1700000000,4200.5,312.0").unwrap();
    assert_eq!(r, reading("inv-a1", 1_700_000_000, 4200.5, 312.0));
}

#[test]
fn rejects_non_numeric_watts() {
    let err = parse_line("site-1", "inv-a1,1700000000,lots,312.0").unwrap_err();
    assert_eq!(err, ParseError::BadNumber("lots".into()));
}

#[test]
fn rejects_line_missing_voltage() {
    let err = parse_line("site-1", "inv-a1,1700000000,4200.5").unwrap_err();
    assert_eq!(err, ParseError::MissingField);
}

#[test]
fn summary_totals_watts() {
    let summary = summarize(vec![reading("inv-a1", 1, 1000.0, 300.0), reading("inv-a2", 1, 500.0, 120.0)]);
    assert_eq!(summary.total_watts, 1500.0);
}

#[test]
fn dedupe_drops_repeated_samples() {
    let out = dedupe(vec![reading("inv-a1", 1, 1.0, 300.0), reading("inv-a1", 1, 1.0, 300.0), reading("inv-a1", 2, 1.0, 300.0)]);
    assert_eq!(out.len(), 2);
}

#[test]
fn watermark_rejects_stale_batches() {
    let marks = Watermarks::default();
    assert!(marks.advance_if_newer("site-1", 10));
    assert!(!marks.advance_if_newer("site-1", 10));
    assert!(marks.advance_if_newer("site-1", 11));
}

struct FixedRating;

impl RatingRegistry for FixedRating {
    fn rated_watts(&self, _inverter_id: &str) -> Option<f64> {
        Some(5000.0)
    }
}

#[test]
fn utilisation_covers_every_inverter() {
    let readings = vec![reading("inv-a1", 1, 2500.0, 300.0), reading("inv-unregistered", 1, 2500.0, 300.0)];
    let util = utilisation(&readings, &FixedRating);
    assert_eq!(util.len(), 2);
    assert_eq!(util[1], ("inv-unregistered".to_string(), 0.5));
}

struct OnePage;

impl VendorApi for OnePage {
    fn fetch_page(&self, _site_id: &str, _cursor: Option<&str>) -> io::Result<Page> {
        Ok(Page {
            lines: vec!["inv-a1,5,100,300".into(), "inv-a1,5,100,300".into(), "inv-a1,6,90,300".into()],
            next_cursor: None,
        })
    }
}

#[test]
fn backfill_parses_vendor_lines() {
    let readings = backfill_site(&OnePage, "site-1").unwrap();
    assert_eq!(readings.len(), 2);
}

#[test]
fn ingest_archives_batch() {
    let state = temp_state("archive");
    let (status, body) = ingest_body(&state, "site-1", "inv-a1,100,2500,300\ninv-a1,101,2600,301\n");
    assert_eq!(status, 202, "{body}");
    let csv = std::fs::read_to_string(state.archive_dir.join("site-1").join("inv-a1.csv")).unwrap();
    assert_eq!(csv.lines().count(), 2);
    let _ = std::fs::remove_dir_all(&state.archive_dir);
}

#[test]
fn ingest_rejects_empty_site_id() {
    let state = temp_state("empty");
    let (status, _) = ingest_body(&state, "", "inv-a1,100,2500,300\n");
    assert_eq!(status, 400);
}
