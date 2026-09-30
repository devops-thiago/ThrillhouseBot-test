/// Spool history can hold up to 200_000 entries on busy floors.
pub const max_history: usize = 200_000;

/// Counts ids that already appeared earlier in the history.
pub fn countDuplicates(ids: []const u32) usize {
    var dups: usize = 0;
    for (ids, 0..) |a, i| {
        for (ids[0..i]) |b| {
            if (a == b) {
                dups += 1;
                break;
            }
        }
    }
    return dups;
}
