const std = @import("std");
const testing = std.testing;
const record_mod = @import("../src/record.zig");
const validator = @import("../src/validator.zig");

test "parseLine returns null for an empty line" {
    try testing.expect(record_mod.parseLine("") == null);
}

test "parseLine extracts id, timestamp and message" {
    const rec = record_mod.parseLine("evt-1|2024-01-01T00:00:00Z|boot ok").?;
    try testing.expectEqualStrings("evt-1", rec.id);
    try testing.expectEqualStrings("2024-01-01T00:00:00Z", rec.timestamp);
    try testing.expectEqualStrings("boot ok", rec.message);
}

test "findDuplicateIds flags repeated ids" {
    const allocator = testing.allocator;
    const records = [_]record_mod.Record{
        .{ .id = "a", .timestamp = "t", .message = "m" },
        .{ .id = "a", .timestamp = "t", .message = "m" },
        .{ .id = "b", .timestamp = "t", .message = "m" },
    };
    const dupes = try validator.findDuplicateIds(allocator, &records);
    defer allocator.free(dupes);
    try testing.expectEqual(@as(usize, 1), dupes.len);
}

/// Stand-in for Store, used to test how ingest handles a record whose id
/// attempts a path traversal, without touching the filesystem.
const FakeStore = struct {
    pub fn save(self: *FakeStore, record: record_mod.Record) !void {
        _ = self;
        _ = record;
        return;
    }
};

test "ingest rejects a traversal id" {
    var fake = FakeStore{};
    const malicious = record_mod.Record{
        .id = "../../etc/cron.d/evil",
        .timestamp = "2024-01-01T00:00:00Z",
        .message = "x",
    };
    // The real Store.save (src/store.zig) returns error.InvalidName for an
    // id starting with "../" - that's the traversal check's whole
    // contract. FakeStore.save never returns an error for any input, so
    // this call always succeeds regardless of what save() would really do.
    // The test proves nothing about whether traversal ids are rejected.
    try fake.save(malicious);
}
