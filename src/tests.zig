const std = @import("std");
const config = @import("config.zig");
const digest = @import("digest.zig");
const metrics = @import("metrics.zig");
const notifier = @import("notifier.zig");
const report = @import("report.zig");

const testing = std.testing;

fn testConfig(ignored: []const []const u8) config.Config {
    return .{
        .metrics_url = "https://ci-metrics.example.com",
        .fleet_id = "fleet-4471",
        .regression_ratio = 0.5,
        .ignored_pools = ignored,
    };
}

fn sample(record_id: []const u8, pool: []const u8, day: []const u8, wait_ms: i64) metrics.Sample {
    return .{
        .record_id = record_id,
        .pool = pool,
        .day = day,
        .wait_ms = wait_ms,
    };
}

test "splitList trims entries and drops empty fields" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();

    const parsed = try config.splitList(arena.allocator(), "canary-arm64, , sandbox ");
    try testing.expectEqual(@as(usize, 2), parsed.len);
    try testing.expectEqualStrings("canary-arm64", parsed[0]);
    try testing.expectEqualStrings("sandbox", parsed[1]);
}

test "dropDuplicates keeps one row per record id" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();

    const samples = [_]metrics.Sample{
        sample("r-1", "linux-x64", "2026-02-01", 4200),
        sample("r-2", "linux-x64", "2026-02-02", 4400),
        sample("r-1", "linux-x64", "2026-02-01", 4200),
    };

    const unique = try report.dropDuplicates(arena.allocator(), &samples);
    try testing.expectEqual(@as(usize, 2), unique.items.len);
    try testing.expectEqualStrings("r-2", unique.items[1].record_id);
}

test "ingest skips ignored pools and warm-runner samples" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const ignored = [_][]const u8{"sandbox"};
    const samples = [_]metrics.Sample{
        sample("r-1", "linux-x64", "2026-02-01", 5000),
        sample("r-2", "linux-x64", "2026-02-02", 40),
        sample("r-3", "linux-x64", "2026-02-03", 5200),
        sample("r-4", "sandbox", "2026-02-03", 900),
    };

    var totals = try report.ingest(alloc, testConfig(&ignored), &samples);
    try testing.expectEqual(@as(usize, 1), totals.count());

    const linux = totals.get("linux-x64").?;
    try testing.expectEqual(@as(usize, 2), linux.days.items.len);
    try testing.expectEqual(@as(i64, 5200), linux.days.items[1]);
}

test "slowPools scores the latest day against the earlier ones" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const cfg = testConfig(&.{});
    const samples = [_]metrics.Sample{
        sample("r-1", "macos-arm64", "2026-02-01", 1000),
        sample("r-2", "macos-arm64", "2026-02-02", 1000),
        sample("r-3", "macos-arm64", "2026-02-03", 3000),
        sample("r-4", "linux-x64", "2026-02-01", 800),
        sample("r-5", "linux-x64", "2026-02-02", 820),
        sample("r-6", "linux-x64", "2026-02-03", 810),
    };

    var totals = try report.ingest(alloc, cfg, &samples);
    const rows = try report.slowPools(alloc, cfg, &totals);

    for (rows.items) |row| {
        if (std.mem.eql(u8, row.pool, "macos-arm64")) {
            try testing.expectEqual(@as(i64, 1000), row.baseline_ms);
            try testing.expectEqual(@as(i64, 3000), row.latest_ms);
            try testing.expect(row.exceeded);
        }
        if (std.mem.eql(u8, row.pool, "linux-x64")) {
            try testing.expect(!row.exceeded);
        }
    }
}

test "render prints the slowdown percentage for each row" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();

    var rows = [_]report.Regression{
        .{ .pool = "macos-arm64", .latest_ms = 3000, .baseline_ms = 1000, .ratio = 2.0, .exceeded = true },
    };

    const lines = try digest.render(arena.allocator(), &rows);
    try testing.expectEqual(@as(usize, 1), lines.len);
    try testing.expect(std.mem.indexOf(u8, lines[0], "macos-arm64: 3000 ms today") != null);
    try testing.expect(std.mem.indexOf(u8, lines[0], "200.0% slower") != null);
}

/// Stands in for the alert channel's token bucket. The production bucket is sized
/// well above the number of digest lines a single run can produce, so a
/// reservation comes back in full.
const StubBucket = struct {
    calls: usize = 0,

    pub fn reserve(self: *StubBucket, want: u32) u32 {
        self.calls += 1;
        return want;
    }
};

test "sendAll drains the digest through the rate limiter, carrying nothing over" {
    var out = std.ArrayList(u8).init(testing.allocator);
    defer out.deinit();

    var bucket = StubBucket{};
    const lines = [_][]const u8{
        "macos-arm64: 3000 ms today vs 1000 baseline (200.0% slower)",
        "linux-x64: 900 ms today vs 810 baseline (11.1% slower)",
        "windows-x64: 5200 ms today vs 5000 baseline (4.0% slower)",
    };

    const sent = try notifier.sendAll(&bucket, out.writer().any(), &lines);

    try testing.expectEqual(@as(usize, 3), sent);
    try testing.expectEqual(@as(usize, 1), bucket.calls);
    try testing.expect(std.mem.indexOf(u8, out.items, "queuewatch: windows-x64:") != null);
}
