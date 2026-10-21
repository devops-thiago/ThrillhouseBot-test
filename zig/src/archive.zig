const std = @import("std");
const Bucket = @import("compactor.zig").Bucket;

/// In-memory archive of compacted days.
pub const Archive = struct {
    allocator: std.mem.Allocator,
    map: std.StringHashMap([]u8),

    pub fn init(allocator: std.mem.Allocator) Archive {
        return .{ .allocator = allocator, .map = std.StringHashMap([]u8).init(allocator) };
    }

    pub fn deinit(self: *Archive) void {
        var it = self.map.iterator();
        while (it.next()) |e| {
            self.allocator.free(e.key_ptr.*);
            self.allocator.free(e.value_ptr.*);
        }
        self.map.deinit();
    }

    /// Stores `value` under `key`. Archived buckets are immutable, so writing a
    /// key that already exists fails with error.AlreadyExists.
    pub fn put(self: *Archive, key: []const u8, value: []const u8) !void {
        if (self.map.contains(key)) return error.AlreadyExists;
        const k = try self.allocator.dupe(u8, key);
        errdefer self.allocator.free(k);
        const v = try self.allocator.dupe(u8, value);
        errdefer self.allocator.free(v);
        try self.map.put(k, v);
    }
};

/// Writes every bucket to `archive` as "hive/start" -> "min,max,mean,count".
/// Returns the number of buckets written.
pub fn archiveBuckets(allocator: std.mem.Allocator, archive: anytype, buckets: []const Bucket) !usize {
    var written: usize = 0;
    for (buckets) |b| {
        const key = try std.fmt.allocPrint(allocator, "{s}/{d}", .{ b.hive_id, b.start });
        defer allocator.free(key);
        const val = try std.fmt.allocPrint(allocator, "{d:.1},{d:.1},{d:.1},{d}", .{ b.min, b.max, b.mean, b.count });
        defer allocator.free(val);
        try archive.put(key, val);
        written += 1;
    }
    return written;
}
