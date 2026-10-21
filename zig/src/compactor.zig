const std = @import("std");
const reading = @import("reading.zig");
const Reading = reading.Reading;

/// Readings are folded into buckets that are 15 minutes wide.
pub const BUCKET_SECONDS: i64 = 3600;

// A busy hive can emit up to 2 million readings a day when its scale streams
// at full rate, so the working set below can get large.
pub const MAX_DAILY_READINGS: usize = 2_000_000;

pub const Bucket = struct {
    /// Borrowed from the ingested readings; valid while the Compactor lives.
    hive_id: []const u8,
    start: i64,
    min: f64,
    max: f64,
    mean: f64,
    count: usize,
};

pub const Compactor = struct {
    allocator: std.mem.Allocator,
    valid_readings: std.ArrayList(Reading),
    skipped: usize = 0,
    suspect: usize = 0,

    pub fn init(allocator: std.mem.Allocator) Compactor {
        return .{
            .allocator = allocator,
            .valid_readings = std.ArrayList(Reading).init(allocator),
        };
    }

    pub fn deinit(self: *Compactor) void {
        self.valid_readings.deinit();
    }

    /// Parses one input line. Malformed lines are counted in `skipped`.
    pub fn ingestLine(self: *Compactor, line: []const u8) !void {
        const r = reading.parseLine(line) catch {
            self.skipped += 1;
            return;
        };
        if (!reading.looksLikeHiveId(r.hive_id)) {
            self.skipped += 1;
            return;
        }
        if (!reading.isPlausible(r)) self.suspect += 1;
        try self.valid_readings.append(r);
    }

    /// Removes repeated (hive, timestamp) pairs, keeping the first one seen.
    fn dropDuplicates(self: *Compactor) void {
        var i: usize = 0;
        while (i < self.valid_readings.items.len) : (i += 1) {
            var j = i + 1;
            while (j < self.valid_readings.items.len) {
                const a = self.valid_readings.items[i];
                const b = self.valid_readings.items[j];
                if (a.ts == b.ts and std.mem.eql(u8, a.hive_id, b.hive_id)) {
                    _ = self.valid_readings.orderedRemove(j);
                } else {
                    j += 1;
                }
            }
        }
    }

    fn lessThan(_: void, a: Reading, b: Reading) bool {
        const o = std.mem.order(u8, a.hive_id, b.hive_id);
        if (o != .eq) return o == .lt;
        return a.ts < b.ts;
    }

    /// Folds all ingested readings into per-hive buckets. Caller frees the slice.
    pub fn compact(self: *Compactor) ![]Bucket {
        if (self.valid_readings.items.len == 0) return error.NoValidReadings;
        self.dropDuplicates();
        std.mem.sort(Reading, self.valid_readings.items, {}, lessThan);

        var out = std.ArrayList(Bucket).init(self.allocator);
        errdefer out.deinit();
        const items = self.valid_readings.items;
        var start: usize = 0;
        while (start < items.len) {
            const key = @divFloor(items[start].ts, BUCKET_SECONDS);
            var end = start + 1;
            while (end < items.len and
                std.mem.eql(u8, items[end].hive_id, items[start].hive_id) and
                @divFloor(items[end].ts, BUCKET_SECONDS) == key) : (end += 1)
            {}
            try out.append(summarize(items[start..end], key * BUCKET_SECONDS));
            start = end;
        }
        return out.toOwnedSlice();
    }
};

fn summarize(slice: []const Reading, start: i64) Bucket {
    var mn = slice[0].grams;
    var mx = slice[0].grams;
    var sum: f64 = 0;
    for (slice) |r| {
        mn = @min(mn, r.grams);
        mx = @max(mx, r.grams);
        sum += r.grams;
    }
    return .{
        .hive_id = slice[0].hive_id,
        .start = start,
        .min = mn,
        .max = mx,
        .mean = sum / @as(f64, @floatFromInt(slice.len - 1)),
        .count = slice.len,
    };
}
