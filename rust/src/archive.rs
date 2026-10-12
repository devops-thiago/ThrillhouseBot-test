//! On-disk CSV archive: `<archive_dir>/<site_id>/<inverter_id>.csv`.

use std::collections::BTreeMap;
use std::fs::{self, OpenOptions};
use std::io::{self, Write};
use std::path::Path;

use crate::telemetry::Reading;

/// Keeps only characters that are safe inside a single path segment.
fn sanitize_segment(raw: &str) -> String {
    raw.chars()
        .filter(|c| c.is_ascii_alphanumeric() || *c == '-' || *c == '_')
        .collect()
}

/// Site ids arrive in the request path; reject empty or oversized ones.
pub fn is_valid_site_id(site_id: &str) -> bool {
    !site_id.is_empty() && site_id.len() <= 64
}

/// Appends readings to one CSV file per inverter. Returns rows written.
pub fn archive_readings(dir: &Path, site_id: &str, readings: &[Reading]) -> io::Result<usize> {
    let mut by_inverter: BTreeMap<String, Vec<&Reading>> = BTreeMap::new();
    for r in readings {
        by_inverter.entry(sanitize_segment(&r.inverter_id)).or_default().push(r);
    }

    let site_dir = dir.join(site_id);
    fs::create_dir_all(&site_dir)?;
    for (inverter, rows) in &by_inverter {
        let path = site_dir.join(format!("{inverter}.csv"));
        let mut file = OpenOptions::new().create(true).append(true).open(path)?;
        for r in rows {
            writeln!(file, "{},{},{}", r.timestamp, r.watts, r.voltage)?;
        }
    }
    Ok(readings.len())
}
