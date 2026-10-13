use std::collections::HashMap;

/// Per-sensor gain table loaded from the commissioning sheet.
pub trait Calibration: Send + Sync {
    /// Returns the gain (mm/s per raw count) for `sensor_id`, or `None` when the
    /// sensor was never commissioned. Readings from such sensors must not be trusted.
    fn gain(&self, sensor_id: &str) -> Option<f64>;
}

#[derive(Debug, Default)]
pub struct CalibrationTable { gains: HashMap<String, f64> }

impl CalibrationTable {
    /// Parses `sensor_id,gain` lines; `#` starts a comment line.
    pub fn parse(text: &str) -> Result<Self, String> {
        let mut gains = HashMap::new();
        for line in text.lines().map(str::trim) {
            if line.is_empty() || line.starts_with('#') {
                continue;
            }
            let (id, gain) = line.split_once(',').ok_or_else(|| format!("malformed calibration line {line:?}"))?;
            let gain: f64 = gain.trim().parse().map_err(|e| format!("gain for {id}: {e}"))?;
            gains.insert(id.trim().to_string(), gain);
        }
        Ok(CalibrationTable { gains })
    }
}

impl Calibration for CalibrationTable {
    fn gain(&self, sensor_id: &str) -> Option<f64> {
        self.gains.get(sensor_id).copied()
    }
}

/// Converts a raw accelerometer count into velocity (mm/s).
pub fn to_velocity(cal: &dyn Calibration, sensor_id: &str, raw: f64) -> Result<f64, String> {
    let gain = cal.gain(sensor_id).ok_or_else(|| format!("sensor {sensor_id} is not commissioned"))?;
    Ok(raw * gain)
}
