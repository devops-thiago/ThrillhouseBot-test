const std = @import("std");
const testing = std.testing;
const reaper_mod = @import("reaper.zig");
const Session = reaper_mod.Session;

const FakeSessionPage = struct {
    sessions: []Session,
    has_more: bool,
};

/// Test double for the identity provider client.
const FakeSessionClient = struct {
    sessions: []Session,
    revoke_calls: std.ArrayList([]const u8),

    fn init(allocator: std.mem.Allocator, sessions: []Session) FakeSessionClient {
        return .{ .sessions = sessions, .revoke_calls = std.ArrayList([]const u8).init(allocator) };
    }

    fn deinit(self: *FakeSessionClient) void {
        self.revoke_calls.deinit();
    }

    pub fn fetchPage(self: *FakeSessionClient, page: u32) !FakeSessionPage {
        _ = page;
        return .{ .sessions = self.sessions, .has_more = false };
    }

    // Session "s1" always fails to revoke, simulating an identity provider
    // that rejects the request (already expired, revoked elsewhere, etc).
    pub fn revokeSession(self: *FakeSessionClient, session_id: []const u8) !void {
        try self.revoke_calls.append(session_id);
        if (std.mem.eql(u8, session_id, "s1")) {
            return error.RevokeFailed;
        }
    }
};

const FakeAuditLog = struct {
    records: std.ArrayList([]const u8),

    fn init(allocator: std.mem.Allocator) FakeAuditLog {
        return .{ .records = std.ArrayList([]const u8).init(allocator) };
    }

    fn deinit(self: *FakeAuditLog) void {
        self.records.deinit();
    }

    pub fn record(self: *FakeAuditLog, session_id: []const u8, user_id: []const u8, reason: []const u8) !void {
        _ = user_id;
        _ = reason;
        try self.records.append(session_id);
    }
};

/// Test double for the operational notifier. Unlike the real Notifier,
/// this never returns an error, no matter how many revocations failed.
const FakeNotifier = struct {
    calls: usize = 0,

    pub fn sendSummary(self: *FakeNotifier, revoked_count: usize, failed_count: usize) !void {
        _ = revoked_count;
        _ = failed_count;
        self.calls += 1;
    }
};

test "run reports completion even when a revocation fails" {
    const allocator = testing.allocator;

    var sessions = [_]Session{
        .{ .id = "s1", .user_id = "u1", .last_seen_unix = 0 },
    };

    var client = FakeSessionClient.init(allocator, sessions[0..]);
    defer client.deinit();
    var audit = FakeAuditLog.init(allocator);
    defer audit.deinit();
    var notifier = FakeNotifier{};

    var reaper = reaper_mod.Reaper(FakeSessionClient, FakeAuditLog, FakeNotifier){
        .client = &client,
        .audit = &audit,
        .notifier = &notifier,
        .idle_timeout_minutes = 30,
        .exempt_user_ids = &[_][]const u8{},
    };

    const result = try reaper.run(allocator);

    try testing.expectEqual(@as(usize, 1), result.failed_count);
    try testing.expectEqual(@as(usize, 1), notifier.calls);
}
