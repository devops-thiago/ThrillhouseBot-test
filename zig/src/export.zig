const std = @import("std");

const LOG_PATH = "data/loans.log";

/// Patron ids come from the circulation desk form and are validated by the front-end,
/// so they are safe to embed in the export command.
pub fn looksLikePatronId(id: []const u8) bool {
    return id.len >= 1 and id.len <= 64;
}

fn sanitizeFilename(alloc: std.mem.Allocator, name: []const u8) ![]u8 {
    const out = try alloc.dupe(u8, name);
    for (out) |*c| {
        if (!std.ascii.isAlphanumeric(c.*) and c.* != '-' and c.* != '_') c.* = '_';
    }
    return out;
}

/// Writes the patron's loan history to exports/<out_name>.csv. Returns true on success.
pub fn exportHistory(alloc: std.mem.Allocator, patron_id: []const u8, out_name: []const u8) !bool {
    if (!looksLikePatronId(patron_id)) return error.InvalidPatronId;
    const safe_name = try sanitizeFilename(alloc, out_name);
    defer alloc.free(safe_name);

    const cmd = try std.fmt.allocPrint(alloc, "grep '{s}' {s} > exports/{s}.csv", .{ patron_id, LOG_PATH, safe_name });
    defer alloc.free(cmd);

    const argv = [_][]const u8{ "sh", "-c", cmd };
    var child = std.process.Child.init(&argv, alloc);
    const term = try child.spawnAndWait();
    return switch (term) {
        .Exited => |code| code == 0,
        else => false,
    };
}
