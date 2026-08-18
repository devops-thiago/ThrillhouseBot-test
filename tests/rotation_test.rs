use shiftdesk::handover::{Handover, HandoverLog, ACK_GRACE_MIN};
use shiftdesk::roster::{parse_absences, parse_rotas, Absence, Rota, RotaDirectory};
use shiftdesk::rotation::{current_shift, next_shift, shift_at, uncovered_rotas, upcoming_shifts};

/// Anchor used by the fixtures: an arbitrary Monday 09:00, in minutes from the epoch.
const ANCHOR_MIN: u64 = 29_310_120;
const DAY_MIN: u64 = 24 * 60;

struct StubRoster {
    rotas: Vec<Rota>,
    absences: Vec<Absence>,
}

impl RotaDirectory for StubRoster {
    fn list_rotas(&self) -> Vec<Rota> {
        self.rotas.clone()
    }

    fn list_absences(&self) -> Vec<Absence> {
        self.absences.clone()
    }
}

fn rota(members: &[&str]) -> Rota {
    Rota {
        id: "net-core".to_string(),
        members: members.iter().map(|m| m.to_string()).collect(),
        shift_len_min: DAY_MIN,
        anchor_min: ANCHOR_MIN,
    }
}

fn absence(engineer: &str, from_min: u64, to_min: u64) -> Absence {
    Absence { engineer: engineer.to_string(), from_min, to_min }
}

#[test]
fn shift_index_follows_the_member_order() {
    let rota = rota(&["ana", "bo", "cleo"]);
    assert_eq!(shift_at(&rota, &[], 0).unwrap().holder, "ana");
    assert_eq!(shift_at(&rota, &[], 1).unwrap().holder, "bo");
    assert_eq!(shift_at(&rota, &[], 3).unwrap().holder, "ana");
}

#[test]
fn current_shift_covers_the_whole_day() {
    let rota = rota(&["ana", "bo"]);
    let shift = current_shift(&rota, &[], ANCHOR_MIN + 300).expect("the rota is running");
    assert_eq!(shift.holder, "ana");
    assert_eq!(shift.start_min, ANCHOR_MIN);
    assert_eq!(shift.end_min, ANCHOR_MIN + DAY_MIN);
}

#[test]
fn nobody_holds_a_rota_before_its_anchor() {
    let rota = rota(&["ana", "bo"]);
    assert!(current_shift(&rota, &[], ANCHOR_MIN - 60).is_none());
}

#[test]
fn a_member_on_leave_passes_the_shift_down_the_order() {
    let rota = rota(&["ana", "bo", "cleo"]);
    let away = vec![absence("ana", ANCHOR_MIN, ANCHOR_MIN + 3 * DAY_MIN)];
    assert_eq!(current_shift(&rota, &away, ANCHOR_MIN + 60).unwrap().holder, "bo");
}

#[test]
fn next_shift_is_the_one_after_the_running_shift() {
    let rota = rota(&["ana", "bo", "cleo"]);
    let shift = next_shift(&rota, &[], ANCHOR_MIN + 60).expect("the rota keeps running");
    assert_eq!(shift.holder, "bo");
    assert_eq!(shift.start_min, ANCHOR_MIN + DAY_MIN);
}

#[test]
fn upcoming_shifts_returns_the_requested_run() {
    let rota = rota(&["ana", "bo"]);
    let shifts = upcoming_shifts(&rota, &[], ANCHOR_MIN, 4);
    assert_eq!(shifts.len(), 4);
    assert_eq!(shifts[3].start_min, ANCHOR_MIN + 3 * DAY_MIN);
}

#[test]
fn a_rota_with_everyone_away_is_reported_as_uncovered() {
    let directory = StubRoster {
        rotas: vec![rota(&["ana", "bo"])],
        absences: vec![
            absence("ana", ANCHOR_MIN, ANCHOR_MIN + DAY_MIN),
            absence("bo", ANCHOR_MIN, ANCHOR_MIN + DAY_MIN),
        ],
    };
    assert_eq!(uncovered_rotas(&directory, ANCHOR_MIN + 30), vec!["net-core".to_string()]);
}

#[test]
fn an_empty_rota_is_reported_as_uncovered() {
    let directory = StubRoster { rotas: vec![rota(&[])], absences: Vec::new() };
    assert_eq!(uncovered_rotas(&directory, ANCHOR_MIN + 30).len(), 1);
}

#[test]
fn only_the_current_holder_can_hand_a_rota_over() {
    let log = HandoverLog::default();
    let entry = Handover::new("net-core", "bo", "cleo", ANCHOR_MIN, "quiet night");
    assert!(log.record(&entry, "ana").is_err());
    assert!(log.latest("net-core").is_none());
}

#[test]
fn a_handover_is_recorded_and_acknowledged() {
    let log = HandoverLog::default();
    let entry = Handover::new("net-core", "ana", "bo", ANCHOR_MIN, "cert renewal still open");
    log.record(&entry, "ana").expect("ana holds the rota");
    log.acknowledge("net-core", "bo", ANCHOR_MIN + 5).expect("bo picks it up");
    assert!(log.latest("net-core").unwrap().acknowledged);
}

#[test]
fn an_acknowledgement_from_the_wrong_engineer_is_refused() {
    let log = HandoverLog::default();
    log.record(&Handover::new("net-core", "ana", "bo", ANCHOR_MIN, ""), "ana").expect("recorded");
    assert!(log.acknowledge("net-core", "cleo", ANCHOR_MIN + 5).is_err());
}

#[test]
fn a_repeated_handover_does_not_land_twice_in_the_log() {
    let log = HandoverLog::default();
    let entry = Handover::new("net-core", "ana", "bo", ANCHOR_MIN, "");
    log.record(&entry, "ana").expect("recorded");
    assert!(log.record(&entry, "ana").is_err());
    assert_eq!(log.for_rota("net-core").len(), 1);
}

#[test]
fn handovers_nobody_picked_up_are_reported_after_the_grace_period() {
    let log = HandoverLog::default();
    log.record(&Handover::new("net-core", "ana", "bo", ANCHOR_MIN, ""), "ana").expect("recorded");
    assert!(log.overdue_acknowledgements(ANCHOR_MIN + ACK_GRACE_MIN - 1).is_empty());
    assert_eq!(log.overdue_acknowledgements(ANCHOR_MIN + ACK_GRACE_MIN).len(), 1);
}

#[test]
fn roster_rows_are_parsed() {
    let body =
        "# page: 1\n# page_size: 1000\n# next_page: 2\nnet-core,1440,29310120,ana|bo|cleo\nstorage,720,29310120,dai\n";
    let rotas = parse_rotas(body);
    assert_eq!(rotas.len(), 2);
    assert_eq!(rotas[0].members, vec!["ana".to_string(), "bo".to_string(), "cleo".to_string()]);
    assert_eq!(rotas[1].shift_len_min, 720);
}

#[test]
fn absence_rows_with_an_empty_window_are_dropped() {
    let absences = parse_absences("ana,29310120,29311560\nbo,29310120,29310120\n");
    assert_eq!(absences.len(), 1);
    assert_eq!(absences[0].engineer, "ana");
}
