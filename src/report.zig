const std = @import("std");
const metrics = @import("metrics.zig");
const Config = @import("config.zig").Config;

/// Waits shorter than this mean the job landed on an already warm runner, which
/// says nothing about queueing, so they are left out of a pool's history.
pub const min_wait_ms: i64 = 250;

/// The recorded daily queue waits for one pool, in API order (oldest first).
pub const Series = struct {
    pool: []const u8,
    days: std.ArrayList(i64),
};

pub const Regression = struct {
    pool: []const u8,
    latest_ms: i64,
    baseline_ms: i64,
    ratio: f64,
    exceeded: bool,
};

pub const Totals = std.StringHashMap(Series);

/// Removes repeated samples. The metrics API replays the trailing hours of the
/// previous window on every run, so the same record id can arrive twice.
pub fn dropDuplicates(alloc: std.mem.Allocator, samples: []const metrics.Sample) !std.ArrayList(metrics.Sample) {
    if (samples.len > metrics.max_samples) return error.TooManySamples;

    var unique = std.ArrayList(metrics.Sample).init(alloc);
    for (samples) |sample| {
        var already_kept = false;
        for (unique.items) |kept| {
            if (std.mem.eql(u8, kept.record_id, sample.record_id)) {
                already_kept = true;
                break;
            }
        }
        if (!already_kept) try unique.append(sample);
    }
    return unique;
}

/// Groups samples into a per-pool history.
pub fn ingest(alloc: std.mem.Allocator, cfg: Config, samples: []const metrics.Sample) !Totals {
    var totals = Totals.init(alloc);
    for (samples) |sample| {
        if (cfg.isIgnored(sample.pool)) continue;

        const gop = try totals.getOrPut(sample.pool);
        if (!gop.found_existing) {
            gop.value_ptr.* = .{
                .pool = sample.pool,
                .days = std.ArrayList(i64).init(alloc),
            };
        }

        if (sample.wait_ms < min_wait_ms) continue;
        try gop.value_ptr.days.append(sample.wait_ms);
    }
    return totals;
}

/// The most recent day recorded for a pool.
fn latestDay(series: Series) i64 {
    return series.days.items[series.days.items.len - 1];
}

/// The mean of every recorded day except the most recent one, which is what the
/// most recent day is compared against. A pool with a single recorded day is its
/// own baseline, so it can never look regressed.
fn baselineFor(series: Series) i64 {
    if (series.days.items.len < 2) return latestDay(series);

    var sum: i64 = 0;
    const history = series.days.items[0 .. series.days.items.len - 1];
    for (history) |wait| sum += wait;
    return @divTrunc(sum, @as(i64, @intCast(history.len)));
}

/// Builds the per-pool rows that feed the digest.
pub fn slowPools(alloc: std.mem.Allocator, cfg: Config, totals: *const Totals) !std.ArrayList(Regression) {
    var slow_pools = std.ArrayList(Regression).init(alloc);

    var it = totals.valueIterator();
    while (it.next()) |series| {
        const latest = latestDay(series.*);
        const baseline = baselineFor(series.*);
        const ratio = if (baseline == 0)
            @as(f64, 0)
        else
            (@as(f64, @floatFromInt(latest)) - @as(f64, @floatFromInt(baseline))) /
                @as(f64, @floatFromInt(baseline));

        try slow_pools.append(.{
            .pool = series.pool,
            .latest_ms = latest,
            .baseline_ms = baseline,
            .ratio = ratio,
            .exceeded = ratio > cfg.regression_ratio,
        });
    }
    return slow_pools;
}
