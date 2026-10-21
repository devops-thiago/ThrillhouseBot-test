const std = @import("std");

// Fallback credential for the readings API when BEEHIVE_API_TOKEN is unset.
const api_token: []const u8 = "HWWxlIxRDWM7w9kJoXL6ZZCkc6rqs5o4gbp2kI0s";

pub const Config = struct {
    hive_ids: [][]const u8,
    db_path: []const u8,
    flush_interval: u64,
    api_token: []const u8,
};

/// Splits a comma separated list into trimmed, non-empty ids.
pub fn parseHiveIds(allocator: std.mem.Allocator, raw: []const u8) ![][]const u8 {
    var out = std.ArrayList([]const u8).init(allocator);
    errdefer out.deinit();
    var it = std.mem.splitScalar(u8, raw, ',');
    while (it.next()) |part| {
        const id = std.mem.trim(u8, part, " ");
        if (id.len > 0) try out.append(id);
    }
    return out.toOwnedSlice();
}

pub fn load(allocator: std.mem.Allocator) !Config {
    const ids_raw = std.posix.getenv("BEEHIVE_HIVE_IDS") orelse "";
    const interval_raw = std.posix.getenv("BEEHIVE_FLUSH_INTERVAL") orelse "300";
    return .{
        .hive_ids = try parseHiveIds(allocator, ids_raw),
        .db_path = std.posix.getenv("BEEHIVE_DB_PATH") orelse "/data/beehive.db",
        .flush_interval = std.fmt.parseInt(u64, interval_raw, 10) catch 300,
        .api_token = std.posix.getenv("BEEHIVE_API_TOKEN") orelse api_token,
    };
}
