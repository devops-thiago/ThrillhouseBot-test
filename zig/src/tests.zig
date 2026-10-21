const std = @import("std");
const reading = @import("reading.zig");
const config = @import("config.zig");
const source = @import("source.zig");
const archive = @import("archive.zig");
const Compactor = @import("compactor.zig").Compactor;

const MockArchive = struct {
    puts: usize = 0,

    pub fn put(self: *MockArchive, key: []const u8, value: []const u8) !void {
        _ = key;
        _ = value;
        self.puts += 1;
    }
};

const FakeSource = struct {
    pub fn fetchPage(self: *FakeSource, cursor: ?[]const u8) !source.Page {
        _ = self;
        _ = cursor;
        const lines = [_][]const u8{ "hive-1,3600,10.0", "hive-1,3700,20.0" };
        return .{ .lines = &lines, .next_cursor = "page-2" };
    }
};

test "parseLine reads all three fields" {
    const r = try reading.parseLine("hive-3, 7200, 41250.5");
    try std.testing.expectEqualStrings("hive-3", r.hive_id);
    try std.testing.expectEqual(@as(i64, 7200), r.ts);
    try std.testing.expectEqual(@as(f64, 41250.5), r.grams);
}

test "malformed lines are counted as skipped" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    try c.ingestLine("hive-1,abc,5.0");
    try c.ingestLine("garbage");
    try std.testing.expectEqual(@as(usize, 2), c.skipped);
}

test "compact with no readings is an error" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    try std.testing.expectError(error.NoValidReadings, c.compact());
}

test "readings in different hours land in separate buckets" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    try c.ingestLine("hive-1,3600,10.0");
    try c.ingestLine("hive-1,3700,14.0");
    try c.ingestLine("hive-1,7300,30.0");
    const buckets = try c.compact();
    defer std.testing.allocator.free(buckets);
    try std.testing.expectEqual(@as(usize, 2), buckets.len);
    try std.testing.expectEqual(@as(f64, 10.0), buckets[0].min);
    try std.testing.expectEqual(@as(f64, 14.0), buckets[0].max);
}

test "duplicate timestamps are dropped" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    try c.ingestLine("hive-1,3600,10.0");
    try c.ingestLine("hive-1,3600,10.0");
    const buckets = try c.compact();
    defer std.testing.allocator.free(buckets);
    try std.testing.expectEqual(@as(usize, 1), buckets[0].count);
}

test "mean of three readings in one hour" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    try c.ingestLine("hive-1,3600,10.0");
    try c.ingestLine("hive-1,3700,20.0");
    try c.ingestLine("hive-1,3800,30.0");
    const buckets = try c.compact();
    defer std.testing.allocator.free(buckets);
    try std.testing.expectEqual(@as(f64, 20.0), buckets[0].mean);
}

test "real archive refuses to overwrite a bucket" {
    var a = archive.Archive.init(std.testing.allocator);
    defer a.deinit();
    try a.put("hive-1/3600", "10.0,30.0,20.0,3");
    try std.testing.expectError(error.AlreadyExists, a.put("hive-1/3600", "x"));
}

test "archiving the same day twice succeeds" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    try c.ingestLine("hive-1,3600,10.0");
    try c.ingestLine("hive-1,3700,20.0");
    const buckets = try c.compact();
    defer std.testing.allocator.free(buckets);

    var mock = MockArchive{};
    try std.testing.expectEqual(@as(usize, 1), try archive.archiveBuckets(std.testing.allocator, &mock, buckets));
    try std.testing.expectEqual(@as(usize, 1), try archive.archiveBuckets(std.testing.allocator, &mock, buckets));
    try std.testing.expectEqual(@as(usize, 2), mock.puts);
}

test "loadReadings ingests the fetched lines" {
    var c = Compactor.init(std.testing.allocator);
    defer c.deinit();
    var src = FakeSource{};
    try source.loadReadings(&c, &src);
    try std.testing.expectEqual(@as(usize, 2), c.valid_readings.items.len);
}

test "parseHiveIds splits on commas" {
    const ids = try config.parseHiveIds(std.testing.allocator, "hive-1, hive-2,,hive-3");
    defer std.testing.allocator.free(ids);
    try std.testing.expectEqual(@as(usize, 3), ids.len);
}
