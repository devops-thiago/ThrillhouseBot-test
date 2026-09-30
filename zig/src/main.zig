const std = @import("std");
const config = @import("config.zig");
const holds = @import("holds.zig");
const catalog = @import("catalog.zig");
const loans = @import("loans.zig");
const notify = @import("notify.zig");

pub fn main() !void {
    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const cfg = try config.load(alloc);
    const titles = try catalog.loadAllTitles(alloc, catalog.SeedCatalog{}, cfg.page_size);
    std.debug.print("catalog titles: {d}\n", .{titles.len});

    var queue = holds.HoldQueue.init(alloc);
    defer queue.deinit();
    const now = std.time.timestamp();
    _ = try queue.enqueue(.{ .patron_id = "p-100", .isbn = "9780441013593", .requested_at = now });
    _ = try queue.enqueue(.{ .patron_id = "p-101", .isbn = "9780441013593", .requested_at = now });

    var mailer = notify.LogMailer{};
    if (queue.markReady(now)) |ready| {
        const outcome = notify.notifyReady(&mailer, "reader@example.org", ready.isbn);
        std.debug.print("notified {s}: {s}\n", .{ ready.patron_id, @tagName(outcome) });
    }

    const active = [_]loans.Loan{.{ .patron_id = "p-100", .isbn = "9780441013593", .due_at = now + 86400 }};
    if (try loans.buildDigest(alloc, &active, now)) |digest| {
        std.debug.print("{s}", .{digest});
    }
    _ = queue.expireReady(now, cfg.hold_ttl_hours);
}
