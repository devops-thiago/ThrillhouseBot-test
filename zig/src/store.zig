const std = @import("std");
const Bucket = @import("compactor.zig").Bucket;

/// Wraps `s` in single quotes for the shell, escaping embedded quotes.
fn shellQuote(allocator: std.mem.Allocator, s: []const u8) ![]u8 {
    var out = std.ArrayList(u8).init(allocator);
    errdefer out.deinit();
    try out.append('\'');
    for (s) |c| {
        if (c == '\'') {
            try out.appendSlice("'\\''");
        } else {
            try out.append(c);
        }
    }
    try out.append('\'');
    return out.toOwnedSlice();
}

/// Inserts one bucket through the sqlite3 command line tool.
/// The hive id was validated when the reading was ingested.
pub fn saveBucket(allocator: std.mem.Allocator, db_path: []const u8, b: Bucket) !void {
    const quoted_db = try shellQuote(allocator, db_path);
    defer allocator.free(quoted_db);
    const cmd = try std.fmt.allocPrint(
        allocator,
        "sqlite3 {s} \"INSERT INTO buckets(hive_id,start_ts,min_g,max_g,mean_g) VALUES ('{s}',{d},{d:.1},{d:.1},{d:.1});\"",
        .{ quoted_db, b.hive_id, b.start, b.min, b.max, b.mean },
    );
    defer allocator.free(cmd);
    const argv = [_][]const u8{ "sh", "-c", cmd };
    var child = std.process.Child.init(&argv, allocator);
    const term = try child.spawnAndWait();
    if (term != .Exited or term.Exited != 0) return error.SqliteFailed;
}
