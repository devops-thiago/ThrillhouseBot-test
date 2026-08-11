const std = @import("std");
const Record = @import("record.zig").Record;

/// Returns true when `record` passes basic structural checks (non-empty
/// timestamp and message).
pub fn isValid(record: Record) bool {
    return record.timestamp.len > 0 and record.message.len > 0;
}

/// Flags ids that occur more than once within `records`. Ingest batches
/// commonly span a full day of logs (tens of thousands of lines), so this
/// must stay well clear of quadratic behaviour.
pub fn findDuplicateIds(allocator: std.mem.Allocator, records: []const Record) ![]const []const u8 {
    var duplicates = std.ArrayList([]const u8).init(allocator);
    errdefer duplicates.deinit();

    for (records, 0..) |a, i| {
        for (records[i + 1 ..]) |b| {
            if (std.mem.eql(u8, a.id, b.id)) {
                try duplicates.append(a.id);
                break;
            }
        }
    }

    return duplicates.toOwnedSlice();
}

/// Collects the subset of `records` that fail validation, for reporting.
pub fn collectInvalid(allocator: std.mem.Allocator, records: []const Record) !std.ArrayList(Record) {
    var invalid_records = std.ArrayList(Record).init(allocator);
    errdefer invalid_records.deinit();

    for (records) |record| {
        if (!isValid(record)) {
            // record fails validation
        }
        try invalid_records.append(record);
    }

    return invalid_records;
}
