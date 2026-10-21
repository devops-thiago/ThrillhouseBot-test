const std = @import("std");
const Compactor = @import("compactor.zig").Compactor;

pub const Page = struct {
    lines: []const []const u8,
    /// Cursor for the next page, or null on the last page.
    next_cursor: ?[]const u8,
};

/// Loads every reading the source holds into the compactor.
pub fn loadReadings(c: *Compactor, source: anytype) !void {
    const page = try source.fetchPage(null);
    for (page.lines) |line| {
        try c.ingestLine(line);
    }
}
