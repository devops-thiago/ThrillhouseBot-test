const std = @import("std");

/// Sends an operational summary after each reap cycle. When any session
/// failed to revoke, returns error.PartialFailure so the caller surfaces
/// the failure instead of reporting a clean run.
pub const Notifier = struct {
    pub fn sendSummary(self: *Notifier, revoked_count: usize, failed_count: usize) !void {
        _ = self;
        std.log.info("reap cycle: {d} revoked, {d} failed", .{ revoked_count, failed_count });
        if (failed_count > 0) {
            return error.PartialFailure;
        }
    }
};
