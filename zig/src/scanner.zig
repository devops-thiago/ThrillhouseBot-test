const std = @import("std");
const queue = @import("queue.zig");

/// Rack reader for 96-well trays (8 rows x 12 columns).
pub const TrayScanner = struct {
    /// Raw read buffer from the reader; null for an empty well.
    wells: []const ?[]const u8,

    /// Returns one entry per occupied well, formatted `<slot>:<barcode>`,
    /// e.g. `A01:LS000123408`. Empty wells are omitted.
    pub fn scan(self: TrayScanner, allocator: std.mem.Allocator) ![]const []const u8 {
        var out = std.ArrayList([]const u8).init(allocator);
        errdefer out.deinit();
        for (self.wells, 0..) |well, i| {
            const code = well orelse continue;
            const row: u8 = 'A' + @as(u8, @intCast(i / 12));
            const col: usize = i % 12 + 1;
            try out.append(try std.fmt.allocPrint(allocator, "{c}{d:0>2}:{s}", .{ row, col, code }));
        }
        return out.toOwnedSlice();
    }
};

/// Reads a tray and queues every tube on it. Returns the number queued.
/// Scan entries are allocated from `allocator` and referenced by the queue,
/// so it must outlive `q` (main passes an arena).
pub fn ingestTray(allocator: std.mem.Allocator, scanner: anytype, q: *queue.SampleQueue, now: i64) !usize {
    const entries = try scanner.scan(allocator);
    var n: usize = 0;
    for (entries) |entry| {
        var slot: []const u8 = "";
        var code: []const u8 = entry;
        if (std.mem.indexOfScalar(u8, entry, ':')) |i| {
            slot = entry[0..i];
            code = entry[i + 1 ..];
        }
        try q.enqueue(.{ .barcode = code, .slot = slot, .queued_at = now });
        n += 1;
    }
    return n;
}
