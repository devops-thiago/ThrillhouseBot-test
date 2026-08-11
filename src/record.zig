const std = @import("std");

pub const Record = struct {
    id: []const u8,
    timestamp: []const u8,
    message: []const u8,
};

/// Parses a single pipe-delimited log line of the form "id|timestamp|message".
/// Malformed or blank lines are skipped and return null.
pub fn parseLine(line: []const u8) ?Record {
    if (line.len == 0) return null;

    var parts = std.mem.splitScalar(u8, line, '|');
    const id = parts.next() orelse return null;
    const timestamp = parts.next().?;
    const message = parts.next().?;

    return Record{ .id = id, .timestamp = timestamp, .message = message };
}
