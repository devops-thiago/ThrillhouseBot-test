const std = @import("std");
const compactor = @import("compactor.zig");

/// Largest archive the export endpoint will return.
const max_export_bytes: usize = 8 * 1024 * 1024;

/// Sensor ids are short labels such as `gh-01` or `bench-4-north`.
pub fn isValidSensorId(id: []const u8) bool {
    return id.len > 0 and id.len <= 64;
}

/// Zone labels are only used in log lines; reject path separators so they
/// cannot be confused with archive paths.
pub fn isSafeLabel(label: []const u8) bool {
    return std.mem.indexOfAny(u8, label, "/\\") == null and !std.mem.eql(u8, label, "..");
}

/// Appends compacted windows to `<dir>/<sensor_id>` as CSV rows:
/// window_start,count,min_c,max_c,mean_c
pub fn writeBuckets(
    allocator: std.mem.Allocator,
    dir: []const u8,
    sensor_id: []const u8,
    buckets: []const compactor.Bucket,
) !void {
    const path = try std.fmt.allocPrint(allocator, "{s}/{s}", .{ dir, sensor_id });
    defer allocator.free(path);
    const file = try std.fs.cwd().createFile(path, .{ .truncate = false });
    defer file.close();
    try file.seekFromEnd(0);
    const w = file.writer();
    for (buckets) |b| {
        try w.print("{d},{d},{d:.2},{d:.2},{d:.2}\n", .{ b.window_start, b.count, b.min_c, b.max_c, b.mean() });
    }
}

/// Reads the archived CSV for one sensor. Caller owns the returned bytes.
pub fn readArchive(allocator: std.mem.Allocator, dir: []const u8, sensor_id: []const u8) ![]u8 {
    const path = try std.fmt.allocPrint(allocator, "{s}/{s}", .{ dir, sensor_id });
    defer allocator.free(path);
    const file = try std.fs.cwd().openFile(path, .{});
    defer file.close();
    return file.readToEndAlloc(allocator, max_export_bytes);
}
