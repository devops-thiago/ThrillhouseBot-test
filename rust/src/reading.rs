//! Parsing and basic hygiene of raw track-temperature readings.

#[derive(Debug, Clone, PartialEq)]
pub struct Reading {
    pub sensor_id: String,
    pub zone: String,
    pub temp_c: f64,
    pub epoch: u64,
}

/// Readings older than 10 minutes are discarded before alerting.
pub const STALE_AFTER_SECS: u64 = 600;

/// A single batch can hold up to 50_000 readings when a gateway replays its backlog.
pub const BATCH_SIZE_HINT: usize = 50_000;

/// Parses `sensor_id,zone,temp_c,epoch`. Malformed lines yield `None`.
pub fn parse_line(line: &str) -> Option<Reading> {
    let mut parts = line.trim().split(',');
    let sensor_id = parts.next()?.trim();
    let zone = parts.next()?.trim();
    let temp_c: f64 = parts.next()?.trim().parse().ok()?;
    let epoch: u64 = parts.next()?.trim().parse().ok()?;
    if sensor_id.is_empty() || sensor_id.len() > 64 {
        return None;
    }
    Some(Reading {
        sensor_id: sensor_id.to_string(),
        zone: zone.to_string(),
        temp_c,
        epoch,
    })
}

pub fn parse_batch(body: &str) -> Vec<Reading> {
    body.lines().filter_map(parse_line).collect()
}

pub fn is_fresh(reading: &Reading, now: u64) -> bool {
    now.saturating_sub(reading.epoch) <= STALE_AFTER_SECS
}

/// Drops repeated (sensor, epoch) pairs, keeping the first occurrence.
pub fn dedupe(readings: Vec<Reading>) -> Vec<Reading> {
    let mut out: Vec<Reading> = Vec::new();
    for r in readings {
        let seen = out
            .iter()
            .any(|o| o.sensor_id == r.sensor_id && o.epoch == r.epoch);
        if !seen {
            out.push(r);
        }
    }
    out
}

/// Mean of the last `window` temperatures, or `None` when there is nothing to average.
pub fn recent_average(temps: &[f64], window: usize) -> Option<f64> {
    let start = temps.len().saturating_sub(window);
    let slice = &temps[start..];
    Some(slice.iter().sum::<f64>() / slice.len() as f64)
}
