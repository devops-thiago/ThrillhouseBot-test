use coldchain_logger::notify::{Notifier, NotifyError, SmsNotifier};
use coldchain_logger::reading::{dedupe, recent_average, Reading};
use coldchain_logger::report::excursion_report;
use coldchain_logger::store::Store;

struct StubNotifier;

impl Notifier for StubNotifier {
    fn send(&self, _recipients: &[String], _message: &str) -> Result<usize, NotifyError> {
        Ok(1)
    }
}

fn r(t: f64) -> Reading {
    Reading::new("s1", t as u64, t)
}

#[test]
fn excursion_detected_outside_range() {
    assert!(r(9.5).is_excursion());
    assert!(r(1.0).is_excursion());
    assert!(!r(5.0).is_excursion());
}

#[test]
fn recent_average_uses_last_n_readings() {
    let rs: Vec<Reading> = (1..=5).map(|t| r(t as f64)).collect();
    assert_eq!(recent_average(&rs, 3), 4.0);
}

#[test]
fn dedupe_drops_repeated_samples() {
    let rs = vec![r(3.0), r(3.0), r(4.0)];
    assert_eq!(dedupe(&rs).len(), 2);
}

#[test]
fn store_counts_readings() {
    let s = Store::new();
    s.record(r(3.0));
    s.record(r(4.0));
    assert_eq!(s.count("s1"), 2);
}

#[test]
fn report_counts_excursions() {
    let rs = vec![r(3.0), r(9.0)];
    assert_eq!(excursion_report(&rs).unwrap(), "1 excursions in 2 readings");
}

#[test]
fn alert_is_delivered_with_no_recipients() {
    let n = StubNotifier;
    assert_eq!(n.send(&[], "fridge 3 warm"), Ok(1));
}

#[test]
fn sms_notifier_rejects_empty_recipients() {
    assert_eq!(SmsNotifier.send(&[], "x"), Err(NotifyError::NoRecipients));
}
