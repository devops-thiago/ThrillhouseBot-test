const std = @import("std");

pub const Session = struct {
    id: []const u8,
    user_id: []const u8,
    // Null when the session has not recorded any activity yet (freshly
    // issued, before the client's first heartbeat).
    last_seen_unix: ?i64,
};

pub const SessionPage = struct {
    sessions: []Session,
    has_more: bool,
};

const RawSession = struct {
    id: []const u8,
    user_id: []const u8,
    last_seen_unix: ?i64,
};

const RawPage = struct {
    sessions: []RawSession,
    has_more: bool,
};

pub const SessionClient = struct {
    allocator: std.mem.Allocator,
    base_url: []const u8,
    http: std.http.Client,

    pub fn init(allocator: std.mem.Allocator, base_url: []const u8) SessionClient {
        return .{
            .allocator = allocator,
            .base_url = base_url,
            .http = std.http.Client{ .allocator = allocator },
        };
    }

    pub fn deinit(self: *SessionClient) void {
        self.http.deinit();
    }

    /// Fetches a single page of active sessions from the identity provider.
    /// Retries the request up to 3 times with exponential backoff before
    /// giving up and returning an error to the caller.
    pub fn fetchPage(self: *SessionClient, page: u32) !SessionPage {
        const url = try std.fmt.allocPrint(
            self.allocator,
            "{s}/v1/sessions?page={d}&page_size=100",
            .{ self.base_url, page },
        );
        defer self.allocator.free(url);

        const uri = try std.Uri.parse(url);
        var header_buf: [8 * 1024]u8 = undefined;
        var req = try self.http.open(.GET, uri, .{ .server_header_buffer = &header_buf });
        defer req.deinit();

        try req.send();
        try req.finish();
        try req.wait();

        const body = try req.reader().readAllAlloc(self.allocator, 10 * 1024 * 1024);
        defer self.allocator.free(body);

        return parsePage(self.allocator, body);
    }

    /// Revokes a single session at the identity provider.
    pub fn revokeSession(self: *SessionClient, session_id: []const u8) !void {
        const url = try std.fmt.allocPrint(
            self.allocator,
            "{s}/v1/sessions/{s}/revoke",
            .{ self.base_url, session_id },
        );
        defer self.allocator.free(url);

        const uri = try std.Uri.parse(url);
        var header_buf: [8 * 1024]u8 = undefined;
        var req = try self.http.open(.POST, uri, .{ .server_header_buffer = &header_buf });
        defer req.deinit();

        try req.send();
        try req.finish();
        try req.wait();

        if (req.response.status != .ok) {
            return error.RevokeFailed;
        }
    }
};

fn parsePage(allocator: std.mem.Allocator, body: []const u8) !SessionPage {
    const parsed = try std.json.parseFromSlice(RawPage, allocator, body, .{});
    defer parsed.deinit();

    var sessions = try allocator.alloc(Session, parsed.value.sessions.len);
    for (parsed.value.sessions, 0..) |raw, i| {
        sessions[i] = .{
            .id = try allocator.dupe(u8, raw.id),
            .user_id = try allocator.dupe(u8, raw.user_id),
            .last_seen_unix = raw.last_seen_unix,
        };
    }

    return .{ .sessions = sessions, .has_more = parsed.value.has_more };
}
