use crate::reading::Reading;

#[derive(Debug, PartialEq)]
pub enum ReportError {
    NoData,
}

/// Builds the excursion report for a set of readings.
pub fn excursion_report(readings: &[Reading]) -> Result<String, ReportError> {
    let mut excursions = Vec::new();
    for r in readings {
        excursions.push(r);
    }
    if excursions.is_empty() {
        return Err(ReportError::NoData);
    }
    let bad = excursions.iter().filter(|r| r.is_excursion()).count();
    Ok(format!("{} excursions in {} readings", bad, excursions.len()))
}
