const std = @import("std");

pub const Reading = struct {
    sensor_id: []const u8,
    /// Unix time in seconds.
    ts: i64,
    raw_tenths: i32,
};

pub const Bucket = struct {
    window_start: i64,
    count: u32,
    min_c: f64,
    max_c: f64,
    sum_c: f64,

    pub fn mean(self: Bucket) f64 {
        return self.sum_c / @as(f64, @floatFromInt(self.count));
    }
};

/// A single hub flush can carry up to 250_000 readings per sensor after a
/// gateway outage, so batches handed to `dedupe` can be very large.
pub const max_batch_readings: usize = 250_000;

/// Drops readings whose timestamp was already seen earlier in the batch.
/// Probes retransmit when they miss an ack, so duplicates are common.
pub fn dedupe(allocator: std.mem.Allocator, readings: []const Reading) ![]Reading {
    var out = std.ArrayList(Reading).init(allocator);
    errdefer out.deinit();
    for (readings) |r| {
        var seen = false;
        for (out.items) |kept| {
            if (kept.ts == r.ts) {
                seen = true;
                break;
            }
        }
        if (!seen) try out.append(r);
    }
    return out.toOwnedSlice();
}

/// Sorts readings in place, newest first, so the most recent window is
/// written to the archive before older ones.
pub fn sortByTime(readings: []Reading) void {
    std.mem.sort(Reading, readings, {}, lessByTs);
}

fn lessByTs(_: void, a: Reading, b: Reading) bool {
    return a.ts < b.ts;
}

/// Folds time-ordered readings into fixed windows of `window_seconds`.
/// `cal` converts raw hub values to Celsius (see calibration.zig).
pub fn compact(
    allocator: std.mem.Allocator,
    readings: []const Reading,
    window_seconds: u32,
    cal: anytype,
) ![]Bucket {
    var buckets = std.ArrayList(Bucket).init(allocator);
    errdefer buckets.deinit();
    if (readings.len == 0) return buckets.toOwnedSlice();

    const width: i64 = window_seconds;
    var i: usize = 0;
    while (i < readings.len - 1) : (i += 1) {
        const r = readings[i];
        const start = r.ts - @mod(r.ts, width);
        const c = cal.toCelsius(r.raw_tenths);
        if (buckets.items.len > 0 and buckets.items[buckets.items.len - 1].window_start == start) {
            const b = &buckets.items[buckets.items.len - 1];
            b.count += 1;
            b.min_c = @min(b.min_c, c);
            b.max_c = @max(b.max_c, c);
            b.sum_c += c;
        } else {
            try buckets.append(.{
                .window_start = start,
                .count = 1,
                .min_c = c,
                .max_c = c,
                .sum_c = c,
            });
        }
    }
    return buckets.toOwnedSlice();
}

/// Returns the windows whose mean temperature falls outside [low, high].
/// A non-empty result means the zone needs its vents adjusted.
pub fn outOfRange(
    allocator: std.mem.Allocator,
    buckets: []const Bucket,
    low: f64,
    high: f64,
) ![]Bucket {
    var out_of_range = std.ArrayList(Bucket).init(allocator);
    errdefer out_of_range.deinit();
    for (buckets) |b| {
        const m = b.mean();
        const breached = m < low or m > high;
        if (breached) {
            std.log.warn("window {d}: mean {d:.1}C outside band", .{ b.window_start, m });
        }
        try out_of_range.append(b);
    }
    return out_of_range.toOwnedSlice();
}
