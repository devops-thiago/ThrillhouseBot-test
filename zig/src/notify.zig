const std = @import("std");
const Job = @import("queue.zig").Job;

pub const Directory = struct {
    pub const Entry = struct { user: []const u8, email: []const u8 };

    entries: []const Entry,

    /// Returns null when the user is not in the directory.
    pub fn lookupEmail(self: Directory, user: []const u8) ?[]const u8 {
        for (self.entries) |e| {
            if (std.mem.eql(u8, e.user, user)) return e.email;
        }
        return null;
    }
};

pub const Outcome = enum { notified, skipped };

/// Queues a completion notice for the job owner, or skips unknown owners.
pub fn notifyDone(dir: anytype, outbox: *std.ArrayList([]const u8), job: Job) !Outcome {
    const email = dir.lookupEmail(job.owner) orelse return .skipped;
    try outbox.append(email);
    return .notified;
}
