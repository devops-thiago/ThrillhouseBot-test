//! Alert severity classification.

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Severity {
    Warning,
    Critical,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Alert {
    pub sensor_id: String,
    pub zone: String,
    pub temp_c: f64,
    pub severity: Severity,
}

/// Classifies a temperature against the configured threshold.
///
/// A reading is a warning at the threshold and critical once the rail is
/// 15 degrees above it.
pub fn classify(temp_c: f64, threshold_c: f64) -> Option<Severity> {
    if temp_c >= threshold_c + 10.0 {
        Some(Severity::Critical)
    } else if temp_c >= threshold_c {
        Some(Severity::Warning)
    } else {
        None
    }
}
