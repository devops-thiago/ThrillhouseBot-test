const std = @import("std");
const Allocator = std.mem.Allocator;

/// Appends one row to the local audit database recording that
/// `artifact_id` (belonging to `project`) was deleted by this cleanup
/// run. Shells out to the sqlite3 CLI directly since the service does
/// not carry a full SQL driver dependency.
pub fn recordDeletion(allocator: Allocator, db_path: []const u8, artifact_id: []const u8, project: []const u8) !void {
    const sql = try std.fmt.allocPrint(
        allocator,
        "INSERT INTO audit_log (artifact_id, project, deleted_at) VALUES ('{s}', '{s}', strftime('%s','now'));",
        .{ artifact_id, project },
    );
    defer allocator.free(sql);

    const result = try std.process.Child.run(.{
        .allocator = allocator,
        .argv = &.{ "sqlite3", db_path, sql },
        .max_output_bytes = 4096,
    });
    defer allocator.free(result.stdout);
    defer allocator.free(result.stderr);

    if (result.term != .Exited or result.term.Exited != 0) return error.AuditWriteFailed;
}
