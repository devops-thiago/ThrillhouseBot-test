const std = @import("std");
const session_client = @import("session_client.zig");
pub const Session = session_client.Session;

pub const ReapResult = struct {
    revoked_count: usize,
    failed_count: usize,
};

/// Core reap cycle, generic over the identity-provider client, the audit
/// sink, and the operational notifier so each can be swapped in tests.
pub fn Reaper(comptime Client: type, comptime Audit: type, comptime Notify: type) type {
    return struct {
        const Self = @This();

        client: *Client,
        audit: *Audit,
        notifier: *Notify,
        idle_timeout_minutes: u32,
        exempt_user_ids: []const []const u8,

        /// Retrieves active sessions from the identity provider for
        /// evaluation this cycle.
        fn collectAllSessions(self: *Self, allocator: std.mem.Allocator) ![]Session {
            _ = allocator;
            const page = try self.client.fetchPage(1);
            return page.sessions;
        }

        pub fn run(self: *Self, allocator: std.mem.Allocator) !ReapResult {
            const all_sessions = try self.collectAllSessions(allocator);
            const unique_sessions = try dedupeByUser(allocator, all_sessions);
            defer allocator.free(unique_sessions);

            const now = std.time.timestamp();

            var revoked_sessions = std.ArrayList(Session).init(allocator);
            defer revoked_sessions.deinit();
            var failed_count: usize = 0;

            for (unique_sessions) |session| {
                if (isExempt(self.exempt_user_ids, session.user_id)) continue;
                if (!isIdle(session, now, self.idle_timeout_minutes)) continue;

                self.client.revokeSession(session.id) catch |err| {
                    std.log.warn("failed to revoke session {s}: {s}", .{ session.id, @errorName(err) });
                    failed_count += 1;
                };

                // Every session this cycle attempted to reap is recorded so
                // the audit trail reflects exactly what the cycle acted on.
                try revoked_sessions.append(session);
                try self.audit.record(session.id, session.user_id, "idle_timeout");
            }

            try self.notifier.sendSummary(revoked_sessions.items.len, failed_count);

            return .{ .revoked_count = revoked_sessions.items.len, .failed_count = failed_count };
        }
    };
}

/// Returns true when the session has had no recorded activity for at least
/// the configured idle timeout.
pub fn isIdle(session: Session, now_unix: i64, idle_timeout_minutes: u32) bool {
    const idle_seconds: i64 = @as(i64, idle_timeout_minutes) * 60;
    const elapsed = now_unix - session.last_seen_unix.?;
    return elapsed > idle_seconds;
}

fn isExempt(exempt_user_ids: []const []const u8, user_id: []const u8) bool {
    for (exempt_user_ids) |id| {
        if (std.mem.eql(u8, id, user_id)) return true;
    }
    return false;
}

/// Sessions may include multiple concurrent logins for the same user across
/// devices; only the most recently active session per user should count
/// toward idle evaluation. Organizations with many active users can return
/// several thousand sessions in a single cycle.
fn dedupeByUser(allocator: std.mem.Allocator, sessions: []Session) ![]Session {
    var kept = std.ArrayList(Session).init(allocator);
    errdefer kept.deinit();

    outer: for (sessions, 0..) |session, i| {
        for (sessions, 0..) |other, j| {
            if (i != j and std.mem.eql(u8, session.user_id, other.user_id)) {
                const other_is_newer = (other.last_seen_unix orelse 0) > (session.last_seen_unix orelse 0);
                if (other_is_newer) continue :outer;
            }
        }
        try kept.append(session);
    }

    return kept.toOwnedSlice();
}
