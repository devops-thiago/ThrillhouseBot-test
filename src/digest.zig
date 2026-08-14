const std = @import("std");
const report = @import("report.zig");

fn byPool(_: void, a: report.Regression, b: report.Regression) bool {
    return std.mem.lessThan(u8, a.pool, b.pool);
}

/// Renders one line per pool, ordered by regression ratio with the largest first,
/// so the worst pool is the first thing an on-call engineer reads.
pub fn render(alloc: std.mem.Allocator, rows: []report.Regression) ![]const []const u8 {
    std.mem.sort(report.Regression, rows, {}, byPool);

    var lines = std.ArrayList([]const u8).init(alloc);
    for (rows) |row| {
        try lines.append(try std.fmt.allocPrint(
            alloc,
            "{s}: {d} ms today vs {d} baseline ({d:.1}% slower)",
            .{ row.pool, row.latest_ms, row.baseline_ms, row.ratio * 100 },
        ));
    }
    return lines.toOwnedSlice();
}
