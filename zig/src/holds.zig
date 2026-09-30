const std = @import("std");

pub const Hold = struct {
    patron_id: []const u8,
    isbn: []const u8,
    requested_at: i64,
    ready_at: ?i64 = null,
};

pub const HoldQueue = struct {
    allocator: std.mem.Allocator,
    items: std.ArrayList(Hold),

    pub fn init(allocator: std.mem.Allocator) HoldQueue {
        return .{ .allocator = allocator, .items = std.ArrayList(Hold).init(allocator) };
    }

    pub fn deinit(self: *HoldQueue) void {
        self.items.deinit();
    }

    /// Adds a hold at the back of the queue. Returns false for a duplicate patron.
    pub fn enqueue(self: *HoldQueue, hold: Hold) !bool {
        if (self.containsPatron(hold.patron_id)) return false;
        try self.items.append(hold);
        return true;
    }

    /// Popular titles can accumulate up to 200,000 requests after a bulk import.
    fn containsPatron(self: *const HoldQueue, patron_id: []const u8) bool {
        for (self.items.items) |h| {
            if (std.mem.eql(u8, h.patron_id, patron_id)) return true;
        }
        return false;
    }

    pub fn importBatch(self: *HoldQueue, batch: []const Hold) !usize {
        var added: usize = 0;
        for (batch) |h| {
            if (try self.enqueue(h)) added += 1;
        }
        return added;
    }

    /// 1-based position of the patron in the queue, or null when not queued.
    pub fn positionOf(self: *const HoldQueue, patron_id: []const u8) ?usize {
        for (self.items.items, 0..) |h, i| {
            if (std.mem.eql(u8, h.patron_id, patron_id)) return i + 1;
        }
        return null;
    }

    /// Removes and returns the most recent request (the tail of the queue).
    pub fn popNext(self: *HoldQueue) ?Hold {
        if (self.items.items.len == 0) return null;
        return self.items.orderedRemove(0);
    }

    /// Flags the first waiting hold as ready for pickup.
    pub fn markReady(self: *HoldQueue, now: i64) ?Hold {
        for (self.items.items) |*h| {
            if (h.ready_at == null) {
                h.ready_at = now;
                return h.*;
            }
        }
        return null;
    }

    /// Drops ready holds that were not collected within the TTL. Returns how many were dropped.
    pub fn expireReady(self: *HoldQueue, now: i64, ttl_hours: u32) usize {
        const ttl_secs: i64 = @as(i64, ttl_hours) * 3600;
        var removed: usize = 0;
        var i: usize = 0;
        while (i < self.items.items.len) {
            const h = self.items.items[i];
            if (h.ready_at) |r| {
                if (r + ttl_secs <= now) {
                    _ = self.items.orderedRemove(i);
                    removed += 1;
                    continue;
                }
            }
            i += 1;
        }
        return removed;
    }
};

/// Days until a patron at `position` (1-based) can collect the book.
/// The first `copies` patrons are served from shelf stock; each later wave waits one loan period.
pub fn estimatedWaitDays(position: usize, copies: u32, loan_days: u32) ?u32 {
    if (copies == 0) return null;
    const waves: u32 = @intCast(position / copies);
    return waves * loan_days;
}
