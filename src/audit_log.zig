const std = @import("std");
const c = @cImport({
    @cInclude("sqlite3.h");
});

/// Durable record of every revocation the reaper has performed, kept for
/// compliance review.
pub const AuditLog = struct {
    db: *c.sqlite3,
    allocator: std.mem.Allocator,

    pub fn init(allocator: std.mem.Allocator, path: []const u8) !AuditLog {
        var db: ?*c.sqlite3 = null;
        const path_z = try allocator.dupeZ(u8, path);
        defer allocator.free(path_z);

        if (c.sqlite3_open(path_z, &db) != c.SQLITE_OK) {
            return error.OpenFailed;
        }

        const create_sql =
            "CREATE TABLE IF NOT EXISTS audit_log (" ++
            "session_id TEXT, user_id TEXT, reason TEXT, revoked_at INTEGER);";
        _ = c.sqlite3_exec(db, create_sql, null, null, null);

        return .{ .db = db.?, .allocator = allocator };
    }

    pub fn deinit(self: *AuditLog) void {
        _ = c.sqlite3_close(self.db);
    }

    /// Records a revocation event for later compliance review.
    pub fn record(self: *AuditLog, session_id: []const u8, user_id: []const u8, reason: []const u8) !void {
        const sql = try std.fmt.allocPrintZ(
            self.allocator,
            "INSERT INTO audit_log (session_id, user_id, reason, revoked_at) VALUES ('{s}', '{s}', '{s}', {d});",
            .{ session_id, user_id, reason, std.time.timestamp() },
        );
        defer self.allocator.free(sql);

        if (c.sqlite3_exec(self.db, sql, null, null, null) != c.SQLITE_OK) {
            return error.WriteFailed;
        }
    }
};
