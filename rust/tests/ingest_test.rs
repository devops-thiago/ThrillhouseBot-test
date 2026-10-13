use turbine_alerts::calibration::{to_velocity, Calibration, CalibrationTable};
use turbine_alerts::config::parse_thresholds;
use turbine_alerts::ingest::{dedup, evaluate, parse_batch, rms_of_last, zone, Reading};
use turbine_alerts::registry::parse_page;

struct FixedGain;

impl Calibration for FixedGain {
    fn gain(&self, _sensor_id: &str) -> Option<f64> {
        Some(0.5)
    }
}

fn reading(sensor: &str, ts_ms: u64, raw: f64) -> Reading {
    Reading { sensor_id: sensor.to_string(), ts_ms, raw }
}

#[test]
fn parses_batch_lines() {
    let batch = parse_batch("acc-1,1000,12.5\n\nacc-1,1001,-3\n").unwrap();
    assert_eq!(batch, vec![reading("acc-1", 1000, 12.5), reading("acc-1", 1001, -3.0)]);
    assert!(parse_batch("acc-1,1000").is_err());
}

#[test]
fn dedup_drops_retransmitted_samples() {
    let unique = dedup(vec![reading("a", 1, 1.0), reading("a", 1, 1.0), reading("b", 1, 2.0)]);
    assert_eq!(unique.len(), 2);
}

#[test]
fn rms_uses_exactly_the_last_window_values() {
    let rms = rms_of_last(&[0.0, 0.0, 3.0, 4.0], 2);
    assert!((rms - 12.5f64.sqrt()).abs() < 1e-9, "got {rms}");
}

#[test]
fn zone_counts_crossed_thresholds() {
    let thresholds = [4.5, 7.1, 11.2];
    assert_eq!(zone(1.0, &thresholds), 0);
    assert_eq!(zone(7.1, &thresholds), 2);
    assert_eq!(zone(30.0, &thresholds), 3);
}

#[test]
fn thresholds_are_sorted() {
    assert_eq!(parse_thresholds("11.2, 4.5,7.1").unwrap(), vec![4.5, 7.1, 11.2]);
    assert!(parse_thresholds(" , ").is_err());
}

#[test]
fn calibration_table_rejects_unknown_sensors() {
    let table = CalibrationTable::parse("# sensor,gain\nacc-1,0.25\n").unwrap();
    assert_eq!(to_velocity(&table, "acc-1", 8.0), Ok(2.0));
    assert!(to_velocity(&table, "acc-9", 8.0).is_err());
}

#[test]
fn converts_raw_counts_for_newly_installed_sensor() {
    assert_eq!(to_velocity(&FixedGain, "acc-new-0042", 10.0), Ok(5.0));
}

#[test]
fn evaluate_reports_zone() {
    let readings = vec![reading("acc-1", 2, 30.0), reading("acc-1", 1, 30.0)];
    let ev = evaluate("wt-07", &readings, &FixedGain, &[4.5, 7.1, 11.2], 16).unwrap();
    assert_eq!(ev.zone, 3);
}

#[test]
fn parses_registry_page_with_cursor() {
    let page = parse_page("wt-01,North Ridge\nwt-02,North Ridge\nnext=abc123\n").unwrap();
    assert_eq!(page.turbines.len(), 2);
    assert_eq!(page.next_page.as_deref(), Some("abc123"));
}
