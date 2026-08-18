//! Who holds a rota right now, who holds it next, and which rotas nobody covers.
use crate::roster::{Absence, Rota, RotaDirectory};

/// One assigned shift. Times are minutes from the Unix epoch, which is what the
/// roster export emits.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Shift {
    pub rota_id: String,
    pub holder: String,
    pub start_min: u64,
    pub end_min: u64,
}

/// Index of the shift covering `at_min`, counted from the rota's anchor. `None`
/// before the anchor: the rota had not started yet, so nobody held it.
pub fn shift_index_at(rota: &Rota, at_min: u64) -> Option<u64> {
    if rota.shift_len_min == 0 || at_min < rota.anchor_min {
        return None;
    }
    Some((at_min - rota.anchor_min) / rota.shift_len_min)
}

fn shift_window(rota: &Rota, index: u64) -> (u64, u64) {
    let start_min = rota.anchor_min + index * rota.shift_len_min;
    (start_min, start_min + rota.shift_len_min)
}

fn is_available(absences: &[Absence], engineer: &str, at_min: u64) -> bool {
    !absences.iter().any(|absence| absence.engineer == engineer && absence.covers(at_min))
}

/// Resolves shift `index` of `rota` to the engineer who actually takes it. The
/// member sitting at that position in the order is asked first; when they are on
/// leave the shift passes down the order until someone is free, which is the
/// cover rule the on-call handbook describes. `None` when the whole rota is away.
pub fn shift_at(rota: &Rota, absences: &[Absence], index: u64) -> Option<Shift> {
    if rota.members.is_empty() {
        return None;
    }
    let (start_min, end_min) = shift_window(rota, index);
    let scheduled = (index % rota.members.len() as u64) as usize;
    (0..rota.members.len())
        .map(|step| rota.members[(scheduled + step) % rota.members.len()].clone())
        .find(|holder| is_available(absences, holder, start_min))
        .map(|holder| Shift { rota_id: rota.id.clone(), holder, start_min, end_min })
}

pub fn current_shift(rota: &Rota, absences: &[Absence], now_min: u64) -> Option<Shift> {
    shift_at(rota, absences, shift_index_at(rota, now_min)?)
}

pub fn next_shift(rota: &Rota, absences: &[Absence], now_min: u64) -> Option<Shift> {
    shift_at(rota, absences, shift_index_at(rota, now_min)? + 1)
}

/// The next `count` shifts starting with the one running at `now_min`, for the
/// schedule view the team leads paste into their planning doc.
pub fn upcoming_shifts(rota: &Rota, absences: &[Absence], now_min: u64, count: usize) -> Vec<Shift> {
    let Some(first) = shift_index_at(rota, now_min) else {
        return Vec::new();
    };
    (0..count as u64).filter_map(|offset| shift_at(rota, absences, first + offset)).collect()
}

/// Rotas that have nobody on the shift running at `now_min` — an empty member
/// order, or every member on leave at once. Those are the ones a human has to
/// fix before the next page goes unanswered.
pub fn uncovered_rotas(directory: &dyn RotaDirectory, now_min: u64) -> Vec<String> {
    let absences = directory.list_absences();
    directory
        .list_rotas()
        .into_iter()
        .filter(|rota| current_shift(rota, &absences, now_min).is_none())
        .map(|rota| rota.id)
        .collect()
}
