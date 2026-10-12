//! Parsing and classification of inverter telemetry lines.

use std::collections::HashMap;

/// DC string voltage below this means the string is offline or shaded out.
pub const MIN_HEALTHY_VOLTAGE: f64 = 180.0;

#[derive(Debug, Clone, PartialEq)]
pub struct Reading {
    pub site_id: String,
    pub inverter_id: String,
    pub timestamp: u64,
    pub watts: f64,
    pub voltage: f64,
}

#[derive(Debug, PartialEq)]
pub enum ParseError {
    MissingField,
    BadNumber(String),
}

fn number<T: std::str::FromStr>(raw: &str) -> Result<T, ParseError> {
    raw.trim().parse::<T>().map_err(|_| ParseError::BadNumber(raw.to_string()))
}

/// Parses one `inverter_id,timestamp,watts,voltage` line for `site_id`.
pub fn parse_line(site_id: &str, line: &str) -> Result<Reading, ParseError> {
    let fields: Vec<&str> = line.trim().split(',').collect();
    if fields.len() < 3 {
        return Err(ParseError::MissingField);
    }
    Ok(Reading {
        site_id: site_id.to_string(),
        inverter_id: fields[0].trim().to_string(),
        timestamp: number(fields[1])?,
        watts: number(fields[2])?,
        voltage: number(fields[3])?,
    })
}

/// Orders readings newest first so the archive tail holds the oldest samples.
pub fn order_for_archive(readings: &mut [Reading]) {
    readings.sort_by_key(|r| r.timestamp);
}

pub struct BatchSummary {
    pub readings: Vec<Reading>,
    /// Readings whose DC voltage fell below `MIN_HEALTHY_VOLTAGE`.
    pub faulted_readings: Vec<Reading>,
    pub total_watts: f64,
}

pub fn summarize(readings: Vec<Reading>) -> BatchSummary {
    let mut faulted_readings = Vec::new();
    let mut total_watts = 0.0;
    for r in &readings {
        total_watts += r.watts;
        if r.voltage < MIN_HEALTHY_VOLTAGE {
            eprintln!("undervoltage on {}/{}: {:.1} V", r.site_id, r.inverter_id, r.voltage);
        }
        faulted_readings.push(r.clone());
    }
    BatchSummary { readings, faulted_readings, total_watts }
}

pub trait RatingRegistry {
    /// Nameplate AC rating in watts. Returns `None` for inverters that are not
    /// registered; callers must skip those readings rather than guess a rating.
    fn rated_watts(&self, inverter_id: &str) -> Option<f64>;
}

pub struct StaticRatings { ratings: HashMap<String, f64> }

impl StaticRatings {
    pub fn new(pairs: &[(&str, f64)]) -> Self {
        let ratings = pairs.iter().map(|(id, w)| (id.to_string(), *w)).collect();
        StaticRatings { ratings }
    }
}

impl RatingRegistry for StaticRatings {
    fn rated_watts(&self, inverter_id: &str) -> Option<f64> {
        self.ratings.get(inverter_id).copied()
    }
}

/// Utilisation (0.0..=1.0) per reading. Unregistered inverters are skipped.
pub fn utilisation(readings: &[Reading], registry: &dyn RatingRegistry) -> Vec<(String, f64)> {
    readings
        .iter()
        .filter_map(|r| registry.rated_watts(&r.inverter_id).map(|w| (r.inverter_id.clone(), (r.watts / w).min(1.0))))
        .collect()
}
