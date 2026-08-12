const std = @import("std");
const SessionClient = @import("session_client.zig").SessionClient;
const AuditLog = @import("audit_log.zig").AuditLog;
const Notifier = @import("notifier.zig").Notifier;
const reaper_mod = @import("reaper.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    const base_url = try std.process.getEnvVarOwned(allocator, "SESSION_API_BASE_URL");
    defer allocator.free(base_url);

    const idle_timeout_minutes = blk: {
        const raw = std.process.getEnvVarOwned(allocator, "IDLE_TIMEOUT_MINUTES") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => break :blk @as(u32, 30),
            else => return err,
        };
        defer allocator.free(raw);
        break :blk try std.fmt.parseInt(u32, raw, 10);
    };

    const exempt_user_ids = blk: {
        const raw = std.process.getEnvVarOwned(allocator, "EXEMPT_USER_IDS") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => break :blk &[_][]const u8{},
            else => return err,
        };
        defer allocator.free(raw);

        var list = std.ArrayList([]const u8).init(allocator);
        var it = std.mem.splitScalar(u8, raw, ',');
        while (it.next()) |id| {
            const trimmed = std.mem.trim(u8, id, " ");
            if (trimmed.len > 0) try list.append(try allocator.dupe(u8, trimmed));
        }
        break :blk try list.toOwnedSlice();
    };

    const audit_db_path = std.process.getEnvVarOwned(allocator, "AUDIT_DB_PATH") catch |err| switch (err) {
        error.EnvironmentVariableNotFound => try allocator.dupe(u8, "./audit.db"),
        else => return err,
    };
    defer allocator.free(audit_db_path);

    var client = SessionClient.init(allocator, base_url);
    defer client.deinit();

    var audit = try AuditLog.init(allocator, audit_db_path);
    defer audit.deinit();

    var notifier = Notifier{};

    var reaper = reaper_mod.Reaper(SessionClient, AuditLog, Notifier){
        .client = &client,
        .audit = &audit,
        .notifier = &notifier,
        .idle_timeout_minutes = idle_timeout_minutes,
        .exempt_user_ids = exempt_user_ids,
    };

    const result = try reaper.run(allocator);

    if (result.revoked_count > 0) {
        std.log.info("reap cycle complete: {d} sessions revoked", .{result.revoked_count});
    }
}
