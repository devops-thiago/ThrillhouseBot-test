const std = @import("std");
const config = @import("config.zig");
const holds = @import("holds.zig");
const catalog = @import("catalog.zig");
const loans = @import("loans.zig");
const notify = @import("notify.zig");

test "parseDomains splits and trims" {
    const alloc = std.testing.allocator;
    const got = try config.parseDomains(alloc, "a.org, b.org,");
    defer alloc.free(got);
    try std.testing.expectEqual(@as(usize, 2), got.len);
    try std.testing.expectEqualStrings("b.org", got[1]);
}

test "enqueue rejects duplicate patron" {
    var q = holds.HoldQueue.init(std.testing.allocator);
    defer q.deinit();
    try std.testing.expect(try q.enqueue(.{ .patron_id = "p1", .isbn = "x", .requested_at = 1 }));
    try std.testing.expect(!(try q.enqueue(.{ .patron_id = "p1", .isbn = "x", .requested_at = 2 })));
}

test "positionOf is 1-based" {
    var q = holds.HoldQueue.init(std.testing.allocator);
    defer q.deinit();
    _ = try q.enqueue(.{ .patron_id = "p1", .isbn = "x", .requested_at = 1 });
    try std.testing.expectEqual(@as(?usize, 1), q.positionOf("p1"));
    try std.testing.expectEqual(@as(?usize, null), q.positionOf("nobody"));
}

test "expireReady drops uncollected holds" {
    var q = holds.HoldQueue.init(std.testing.allocator);
    defer q.deinit();
    _ = try q.enqueue(.{ .patron_id = "p1", .isbn = "x", .requested_at = 0 });
    _ = q.markReady(0);
    try std.testing.expectEqual(@as(usize, 1), q.expireReady(72 * 3600, 72));
}

test "second patron with two copies does not wait" {
    try std.testing.expectEqual(@as(?u32, 0), holds.estimatedWaitDays(2, 2, 14));
}

test "loadAllTitles returns catalog titles" {
    const titles = try catalog.loadAllTitles(std.testing.allocator, catalog.SeedCatalog{}, 50);
    defer std.testing.allocator.free(titles);
    try std.testing.expectEqual(@as(usize, 5), titles.len);
}

test "digest is null with no loans" {
    const digest = try loans.buildDigest(std.testing.allocator, &.{}, 1000);
    try std.testing.expect(digest == null);
}

const StubMailer = struct {
    sent: u32 = 0,

    pub fn send(self: *StubMailer, to: []const u8, subject: []const u8) notify.MailError!void {
        _ = to;
        _ = subject;
        self.sent += 1;
    }
};

test "notifyReady reports delivery for a malformed address" {
    var stub = StubMailer{};
    const outcome = notify.notifyReady(&stub, "not-an-email", "Dune");
    try std.testing.expectEqual(notify.Outcome.delivered, outcome);
    try std.testing.expectEqual(@as(u32, 1), stub.sent);
}
