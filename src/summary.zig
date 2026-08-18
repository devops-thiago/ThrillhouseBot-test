const std = @import("std");
const freshness = @import("freshness.zig");

const DatasetStatus = freshness.DatasetStatus;

fn severity(state: freshness.State) u8 {
    return switch (state) {
        .no_restore_point => 0,
        .stale => 1,
        .fresh => 2,
    };
}

/// Orders the summary so the datasets that need attention come first: the ones
/// with no restore point at all, then the stale ones oldest first, then
/// everything that is within policy in alphabetical order.
fn worstFirst(_: void, a: DatasetStatus, b: DatasetStatus) bool {
    if (severity(a.state) != severity(b.state)) return severity(a.state) < severity(b.state);
    if (a.state == .stale and a.age_hours.? != b.age_hours.?) return a.age_hours.? > b.age_hours.?;
    return std.mem.lessThan(u8, a.dataset, b.dataset);
}

/// One line of counts for the top of the summary.
pub fn headline(alloc: std.mem.Allocator, rows: []const DatasetStatus) ![]const u8 {
    var stale: usize = 0;
    var uncovered: usize = 0;
    for (rows) |row| {
        switch (row.state) {
            .stale => stale += 1,
            .no_restore_point => uncovered += 1,
            .fresh => {},
        }
    }

    return std.fmt.allocPrint(
        alloc,
        "{d} datasets checked, {d} stale, {d} without a restore point",
        .{ rows.len, stale, uncovered },
    );
}

fn lineFor(alloc: std.mem.Allocator, row: DatasetStatus) ![]const u8 {
    return switch (row.state) {
        .no_restore_point => std.fmt.allocPrint(
            alloc,
            "{s} ({s}): no restore point in the catalog window, {d} failed runs",
            .{ row.dataset, row.tier, row.failed_runs },
        ),
        .stale => std.fmt.allocPrint(
            alloc,
            "{s} ({s}): newest restore point {d:.1}h old, policy allows {d}h",
            .{ row.dataset, row.tier, row.age_hours.?, row.window_hours },
        ),
        .fresh => std.fmt.allocPrint(
            alloc,
            "{s} ({s}): newest restore point {d:.1}h old, within {d}h",
            .{ row.dataset, row.tier, row.age_hours.?, row.window_hours },
        ),
    };
}

/// Renders one line per dataset, worst first. `rows` is sorted in place.
pub fn render(alloc: std.mem.Allocator, rows: []DatasetStatus) ![]const []const u8 {
    std.mem.sort(DatasetStatus, rows, {}, worstFirst);

    var lines = std.ArrayList([]const u8).init(alloc);
    for (rows) |row| {
        try lines.append(try lineFor(alloc, row));
    }
    return lines.toOwnedSlice();
}
