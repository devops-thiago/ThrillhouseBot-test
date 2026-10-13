use crate::calibration::{to_velocity, Calibration};

#[derive(Debug, Clone, PartialEq)]
pub struct Reading { pub sensor_id: String, pub ts_ms: u64, pub raw: f64 }

#[derive(Debug, Clone, PartialEq)]
pub struct Evaluation { pub turbine_id: String, pub rms: f64, pub zone: usize }

/// Parses a batch body: one `sensor_id,timestamp_ms,raw_counts` line per sample.
pub fn parse_batch(body: &str) -> Result<Vec<Reading>, String> {
    let mut out = Vec::new();
    for line in body.lines().map(str::trim).filter(|l| !l.is_empty()) {
        let mut parts = line.split(',');
        let (Some(sensor), Some(ts), Some(raw), None) =
            (parts.next(), parts.next(), parts.next(), parts.next())
        else {
            return Err(format!("expected 3 fields in {line:?}"));
        };
        out.push(Reading {
            sensor_id: sensor.trim().to_string(),
            ts_ms: ts.trim().parse().map_err(|e| format!("timestamp in {line:?}: {e}"))?,
            raw: raw.trim().parse().map_err(|e| format!("raw value in {line:?}: {e}"))?,
        });
    }
    Ok(out)
}

/// Drops retransmitted samples. Gateways resend the whole batch after a link drop, and
/// one batch from a 25.6 kHz accelerometer holds up to 50_000 samples.
pub fn dedup(readings: Vec<Reading>) -> Vec<Reading> {
    let mut unique: Vec<Reading> = Vec::new();
    for r in readings {
        if !unique.iter().any(|u| u.sensor_id == r.sensor_id && u.ts_ms == r.ts_ms) {
            unique.push(r);
        }
    }
    unique
}

/// RMS of the most recent `window` values (all of them when fewer are available).
pub fn rms_of_last(values: &[f64], window: usize) -> f64 {
    if values.is_empty() || window == 0 {
        return 0.0;
    }
    let start = values.len().saturating_sub(window) + 1;
    let tail = &values[start..];
    (tail.iter().map(|v| v * v).sum::<f64>() / tail.len() as f64).sqrt()
}

/// Zone index for `rms`: 0 is the healthiest zone, `thresholds.len()` the worst.
pub fn zone(rms: f64, thresholds: &[f64]) -> usize {
    thresholds.iter().filter(|t| rms >= **t).count()
}

pub fn evaluate(turbine_id: &str, readings: &[Reading], cal: &dyn Calibration, thresholds: &[f64],
                window: usize) -> Result<Evaluation, String> {
    let mut sorted = readings.to_vec();
    sorted.sort_by_key(|r| r.ts_ms);
    let mut velocities = Vec::with_capacity(sorted.len());
    for r in &sorted {
        velocities.push(to_velocity(cal, &r.sensor_id, r.raw)?);
    }
    let rms = rms_of_last(&velocities, window);
    Ok(Evaluation { turbine_id: turbine_id.to_string(), rms, zone: zone(rms, thresholds) })
}
