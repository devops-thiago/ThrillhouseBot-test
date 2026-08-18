const std = @import("std");
const catalog = @import("catalog.zig");
const config = @import("config.zig");
const freshness = @import("freshness.zig");
const policy = @import("policy.zig");
const status = @import("status.zig");
const summary = @import("summary.zig");

const testing = std.testing;

const hour: i64 = 3600;
const now: i64 = 1_770_000_000;

fn testConfig(windows: []const policy.TierWindow, exempt: []const []const u8) config.Config {
    return .{
        .catalog_url = "https://backup-catalog.example.com",
        .estate_id = "estate-eu-1",
        .tier_windows = windows,
        .default_window_hours = 24,
        .exempt_datasets = exempt,
        .status_path = "/var/lib/staleguard/status.json",
    };
}

fn job(job_id: []const u8, dataset: []const u8, tier: []const u8, outcome: []const u8, finished_at: i64) catalog.Job {
    return .{
        .job_id = job_id,
        .dataset = dataset,
        .tier = tier,
        .outcome = outcome,
        .finished_at = finished_at,
        .bytes_written = 4096,
    };
}

test "splitList trims entries and drops empty fields" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();

    const parsed = try config.splitList(arena.allocator(), "scratch-vol, , ci-artifacts ");
    try testing.expectEqual(@as(usize, 2), parsed.len);
    try testing.expectEqualStrings("scratch-vol", parsed[0]);
    try testing.expectEqualStrings("ci-artifacts", parsed[1]);
}

test "tier policy parses windows and rejects malformed fields" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const windows = try policy.parse(alloc, "gold=6, silver=24 , archive=168");
    try testing.expectEqual(@as(usize, 3), windows.len);
    try testing.expectEqualStrings("silver", windows[1].tier);
    try testing.expectEqual(@as(i64, 168), windows[2].hours);

    try testing.expectError(error.MalformedTierPolicy, policy.parse(alloc, "gold"));
    try testing.expectError(error.InvalidWindow, policy.parse(alloc, "gold=0"));
}

test "windowFor falls back to the default for an unlisted tier" {
    const windows = [_]policy.TierWindow{.{ .tier = "gold", .hours = 6 }};
    const cfg = testConfig(&windows, &.{});

    try testing.expectEqual(@as(i64, 6), cfg.windowFor("gold"));
    try testing.expectEqual(@as(i64, 24), cfg.windowFor("bronze"));
}

test "dropDuplicates keeps one record per job id" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();

    const jobs = [_]catalog.Job{
        job("bj-1", "billing-ledger", "gold", "succeeded", now - 2 * hour),
        job("bj-2", "billing-ledger", "gold", "failed", now - hour),
        job("bj-1", "billing-ledger", "gold", "succeeded", now - 2 * hour),
    };

    const unique = try freshness.dropDuplicates(arena.allocator(), &jobs);
    try testing.expectEqual(@as(usize, 2), unique.items.len);
    try testing.expectEqualStrings("bj-2", unique.items[1].job_id);
}

test "fold keeps the newest restore point and the tier of the newest run" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const exempt = [_][]const u8{"ci-artifacts"};
    const jobs = [_]catalog.Job{
        job("bj-1", "billing-ledger", "silver", "succeeded", now - 30 * hour),
        job("bj-2", "billing-ledger", "gold", "succeeded", now - 5 * hour),
        job("bj-3", "billing-ledger", "gold", "failed", now - 2 * hour),
        job("bj-4", "ci-artifacts", "bronze", "succeeded", now - hour),
    };

    var states = try freshness.fold(alloc, testConfig(&.{}, &exempt), &jobs);
    try testing.expectEqual(@as(usize, 1), states.count());

    const ledger = states.get("billing-ledger").?;
    try testing.expectEqualStrings("gold", ledger.tier);
    try testing.expectEqual(@as(i64, now - 5 * hour), ledger.last_success.?);
    try testing.expectEqual(@as(usize, 1), ledger.failedSinceSuccess());
}

test "failures recorded before the newest restore point are not counted" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const jobs = [_]catalog.Job{
        job("bj-1", "media-index", "bronze", "failed", now - 40 * hour),
        job("bj-2", "media-index", "bronze", "cancelled", now - 36 * hour),
        job("bj-3", "media-index", "bronze", "succeeded", now - 10 * hour),
    };

    var states = try freshness.fold(alloc, testConfig(&.{}, &.{}), &jobs);
    try testing.expectEqual(@as(usize, 0), states.get("media-index").?.failedSinceSuccess());
}

test "evaluate applies the tier window to each dataset" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const windows = [_]policy.TierWindow{.{ .tier = "gold", .hours = 6 }};
    const cfg = testConfig(&windows, &.{});
    const jobs = [_]catalog.Job{
        job("bj-1", "billing-ledger", "gold", "succeeded", now - 9 * hour),
        job("bj-2", "media-index", "bronze", "succeeded", now - 9 * hour),
        job("bj-3", "audit-trail", "gold", "failed", now - 3 * hour),
    };

    var states = try freshness.fold(alloc, cfg, &jobs);
    const rows = try freshness.evaluate(alloc, cfg, &states, now);
    try testing.expectEqual(@as(usize, 2), freshness.breaching(rows.items));

    for (rows.items) |row| {
        if (std.mem.eql(u8, row.dataset, "billing-ledger")) {
            try testing.expectEqual(freshness.State.stale, row.state);
            try testing.expectEqual(@as(i64, 6), row.window_hours);
            try testing.expectApproxEqAbs(@as(f64, 9), row.age_hours.?, 0.01);
        }
        if (std.mem.eql(u8, row.dataset, "media-index")) {
            try testing.expectEqual(freshness.State.fresh, row.state);
        }
        if (std.mem.eql(u8, row.dataset, "audit-trail")) {
            try testing.expectEqual(freshness.State.no_restore_point, row.state);
            try testing.expectEqual(@as(?f64, null), row.age_hours);
            try testing.expectEqual(@as(usize, 1), row.failed_runs);
        }
    }
}

test "render puts the datasets needing attention first" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    var rows = [_]freshness.DatasetStatus{
        .{ .dataset = "media-index", .tier = "bronze", .state = .fresh, .age_hours = 3.5, .window_hours = 72, .failed_runs = 0 },
        .{ .dataset = "billing-ledger", .tier = "gold", .state = .stale, .age_hours = 9.0, .window_hours = 6, .failed_runs = 1 },
        .{ .dataset = "audit-trail", .tier = "gold", .state = .no_restore_point, .age_hours = null, .window_hours = 6, .failed_runs = 4 },
        .{ .dataset = "customer-blobs", .tier = "silver", .state = .stale, .age_hours = 51.0, .window_hours = 24, .failed_runs = 0 },
    };

    const lines = try summary.render(alloc, &rows);
    try testing.expectEqual(@as(usize, 4), lines.len);
    try testing.expectEqualStrings(
        "audit-trail (gold): no restore point in the catalog window, 4 failed runs",
        lines[0],
    );
    try testing.expectEqualStrings(
        "customer-blobs (silver): newest restore point 51.0h old, policy allows 24h",
        lines[1],
    );
    try testing.expect(std.mem.startsWith(u8, lines[2], "billing-ledger"));
    try testing.expect(std.mem.endsWith(u8, lines[3], "3.5h old, within 72h"));

    const counts = try summary.headline(alloc, &rows);
    try testing.expectEqualStrings("4 datasets checked, 2 stale, 1 without a restore point", counts);
}

test "status document carries the whole sweep" {
    var arena = std.heap.ArenaAllocator.init(testing.allocator);
    defer arena.deinit();

    const rows = [_]freshness.DatasetStatus{
        .{ .dataset = "billing-ledger", .tier = "gold", .state = .stale, .age_hours = 9.0, .window_hours = 6, .failed_runs = 1 },
        .{ .dataset = "media-index", .tier = "bronze", .state = .fresh, .age_hours = 3.5, .window_hours = 72, .failed_runs = 0 },
    };

    const body = try status.render(arena.allocator(), testConfig(&.{}, &.{}), &rows, now);

    try testing.expect(std.mem.indexOf(u8, body, "\"estate\":\"estate-eu-1\"") != null);
    try testing.expect(std.mem.indexOf(u8, body, "\"datasets_checked\":2") != null);
    try testing.expect(std.mem.indexOf(u8, body, "\"datasets_breaching\":1") != null);
    try testing.expect(std.mem.indexOf(u8, body, "\"state\":\"stale\"") != null);
    try testing.expect(std.mem.indexOf(u8, body, "\"window_hours\":72") != null);
}
