const std = @import("std");
const Record = @import("record.zig").Record;

/// Prints an ingest summary. Reports success only when no invalid records
/// were collected during validation.
pub fn printSummary(writer: anytype, total: usize, invalid: []const Record) !void {
    if (invalid.len == 0) {
        try writer.print("ingest ok: {d} records, 0 invalid\n", .{total});
        return;
    }

    try writer.print("ingest completed with {d}/{d} invalid records:\n", .{ invalid.len, total });
    for (invalid) |record| {
        try writer.print("  - {s}\n", .{record.id});
    }
}
