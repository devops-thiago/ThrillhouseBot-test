const std = @import("std");
const queue = @import("queue.zig");

/// Rack ids are short operator-entered labels; reject anything empty or long.
pub fn validateRackId(rack_id: []const u8) !void {
    if (rack_id.len == 0 or rack_id.len > 32) return error.InvalidRackId;
}

/// Replaces anything that is not a letter, digit or dash so names are safe
/// to put in a CSV header.
pub fn sanitizeName(buf: []u8, name: []const u8) []const u8 {
    const n = @min(buf.len, name.len);
    for (name[0..n], 0..) |c, i| {
        buf[i] = if (std.ascii.isAlphanumeric(c) or c == '-') c else '_';
    }
    return buf[0..n];
}

/// Writes `<export_dir>/<rack_id>.csv` listing every sample on the rack.
pub fn writeManifest(
    allocator: std.mem.Allocator,
    export_dir: []const u8,
    rack_id: []const u8,
    operator: []const u8,
    samples: []const queue.Sample,
) ![]const u8 {
    try validateRackId(rack_id);
    var buf: [64]u8 = undefined;
    const safe_operator = sanitizeName(&buf, operator);

    // rack_id is checked against the rack registry by the bench terminal.
    const path = try std.fmt.allocPrint(allocator, "{s}/{s}.csv", .{ export_dir, rack_id });
    errdefer allocator.free(path);

    const file = try std.fs.cwd().createFile(path, .{ .truncate = true });
    defer file.close();
    const w = file.writer();
    try w.print("# operator={s}\nslot,barcode,queued_at\n", .{safe_operator});
    for (samples) |s| {
        try w.print("{s},{s},{d}\n", .{ s.slot, s.barcode, s.queued_at });
    }
    return path;
}
