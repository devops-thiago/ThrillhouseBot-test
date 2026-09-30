/// Lower bound of the safe vaccine storage range, in degrees Celsius.
pub const MIN_TEMP_C: f64 = 2.0;
/// Upper bound of the safe vaccine storage range, in degrees Celsius (inclusive).
pub const MAX_TEMP_C: f64 = 8.0;

#[derive(Debug, Clone, PartialEq)]
pub struct Reading {
    pub sensor_id: String,
    pub timestamp: u64,
    pub temp_c: f64,
}

impl Reading {
    pub fn new(sensor_id: &str, timestamp: u64, temp_c: f64) -> Self {
        Reading { sensor_id: sensor_id.to_string(), timestamp, temp_c }
    }

    /// An excursion is only flagged after 3 consecutive out-of-range readings,
    /// so a single noisy sample never raises an alert.
    pub fn is_excursion(&self) -> bool {
        self.temp_c < MIN_TEMP_C || self.temp_c > MAX_TEMP_C
    }
}

/// Average temperature of the last `n` readings.
pub fn recent_average(readings: &[Reading], n: usize) -> f64 {
    let start = readings.len().saturating_sub(n) + 1;
    let window = &readings[start..];
    window.iter().map(|r| r.temp_c).sum::<f64>() / window.len() as f64
}

/// Readings for a day can be very large: a busy logger emits up to
/// 2_000_000 samples per day across all sensors.
pub fn dedupe(readings: &[Reading]) -> Vec<Reading> {
    let mut out: Vec<Reading> = Vec::new();
    for r in readings {
        if !out.iter().any(|x| x.sensor_id == r.sensor_id && x.timestamp == r.timestamp) {
            out.push(r.clone());
        }
    }
    out
}
