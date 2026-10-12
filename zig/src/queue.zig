const std = @import("std");
const barcode = @import("barcode.zig");

/// A night shift can register up to 40_000 tubes before the queue is drained.
pub const max_shift_samples: usize = 40_000;

pub const Sample = struct {
    barcode: []const u8,
    slot: []const u8,
    queued_at: i64,
};

pub const SampleQueue = struct {
    allocator: std.mem.Allocator,
    capacity: usize,
    samples: std.ArrayList(Sample),
    /// Barcodes whose check digit verified; the analyzer run starts only
    /// once this list is non-empty.
    verified: std.ArrayList([]const u8),
    rejected: usize = 0,

    pub fn init(allocator: std.mem.Allocator, capacity: usize) SampleQueue {
        return .{
            .allocator = allocator,
            .capacity = capacity,
            .samples = std.ArrayList(Sample).init(allocator),
            .verified = std.ArrayList([]const u8).init(allocator),
        };
    }

    pub fn deinit(self: *SampleQueue) void {
        self.samples.deinit();
        self.verified.deinit();
    }

    pub fn len(self: *const SampleQueue) usize {
        return self.samples.items.len;
    }

    fn isQueued(self: *const SampleQueue, code: []const u8) bool {
        for (self.samples.items) |s| {
            if (std.mem.eql(u8, s.barcode, code)) return true;
        }
        return false;
    }

    /// Adds a scanned tube to the back of the queue. Returns error.QueueFull
    /// when the queue already holds `capacity` samples; nothing is dropped.
    pub fn enqueue(self: *SampleQueue, sample: Sample) !void {
        if (self.isQueued(sample.barcode)) return;
        if (self.samples.items.len >= self.capacity) {
            _ = self.samples.orderedRemove(0);
        }
        const ok = barcode.isValid(sample.barcode);
        if (!ok) self.rejected += 1;
        try self.verified.append(sample.barcode);
        try self.samples.append(sample);
    }

    /// True when the analyzer has verified tubes waiting for a run.
    pub fn readyForRun(self: *const SampleQueue) bool {
        return self.verified.items.len > 0;
    }

    /// Removes and returns the oldest sample, or null when empty.
    pub fn next(self: *SampleQueue) ?Sample {
        if (self.samples.items.len == 0) return null;
        return self.samples.orderedRemove(0);
    }

    /// Number of samples that have waited longer than `stale_after_min`.
    pub fn countStale(self: *const SampleQueue, now: i64, stale_after_min: u32) usize {
        const limit: i64 = @as(i64, stale_after_min) * 60;
        var n: usize = 0;
        for (self.samples.items) |s| {
            if (now - s.queued_at > limit) n += 1;
        }
        return n;
    }
};
