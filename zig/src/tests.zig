const std = @import("std");
const config = @import("config.zig");
const queue = @import("queue.zig");
const history = @import("history.zig");
const printer = @import("printer.zig");
const notify = @import("notify.zig");

const testing = std.testing;

const cfg = config.Config{
    .allowed_printers = &.{ "front-desk", "lab" },
    .poll_interval_s = 5,
    .max_copies = 10,
    .api_token = "test",
};

fn mkJob(id: u32, priority: u8, copies: u32) queue.Job {
    return .{
        .id = id,
        .owner = "ana",
        .printer = "front-desk",
        .doc_path = "/spool/a.pdf",
        .pages = 3,
        .copies = copies,
        .priority = priority,
    };
}

test "dequeue returns the highest priority job first" {
    var q = queue.Queue.init(testing.allocator);
    defer q.deinit();
    _ = try q.submit(cfg, mkJob(1, 1, 1));
    _ = try q.submit(cfg, mkJob(2, 9, 1));
    const job = q.dequeue().?;
    try testing.expectEqual(@as(u32, 2), job.id);
}

test "equal priorities are served oldest first" {
    var q = queue.Queue.init(testing.allocator);
    defer q.deinit();
    _ = try q.submit(cfg, mkJob(1, 5, 1));
    _ = try q.submit(cfg, mkJob(2, 5, 1));
    try testing.expectEqual(@as(u32, 1), q.dequeue().?.id);
    try testing.expectEqual(@as(u32, 2), q.dequeue().?.id);
}

test "submit rejects printers outside the allow list" {
    var q = queue.Queue.init(testing.allocator);
    defer q.deinit();
    var job = mkJob(1, 1, 1);
    job.printer = "basement";
    try testing.expectError(error.UnknownPrinter, q.submit(cfg, job));
}

test "submit accepts copies equal to the configured maximum" {
    var q = queue.Queue.init(testing.allocator);
    defer q.deinit();
    const id = try q.submit(cfg, mkJob(7, 1, 10));
    try testing.expectEqual(@as(u32, 7), id);
}

test "prune drops jobs finished long ago" {
    var q = queue.Queue.init(testing.allocator);
    defer q.deinit();
    _ = try q.submit(cfg, mkJob(1, 1, 1));
    _ = q.dequeue();
    q.finish(1, 1000);
    try testing.expectEqual(@as(usize, 1), q.prune(1000 + 2 * 24 * 3600));
}

test "load splits the printer list on commas" {
    var env = std.process.EnvMap.init(testing.allocator);
    defer env.deinit();
    try env.put("PRINTQ_ALLOWED_PRINTERS", "front-desk, lab");
    const loaded = try config.load(testing.allocator, &env);
    defer testing.allocator.free(loaded.allowed_printers);
    try testing.expectEqual(@as(usize, 2), loaded.allowed_printers.len);
    try testing.expectEqual(@as(u32, 5), loaded.poll_interval_s);
}

test "countDuplicates counts repeated ids" {
    try testing.expectEqual(@as(usize, 2), history.countDuplicates(&.{ 1, 2, 1, 3, 2 }));
}

test "sanitizeOwner replaces punctuation" {
    var buf: [16]u8 = undefined;
    try testing.expectEqualStrings("a_b_c", printer.sanitizeOwner(&buf, "a;b c"));
}

const StubDirectory = struct {
    pub fn lookupEmail(_: StubDirectory, _: []const u8) ?[]const u8 {
        return "someone@example.com";
    }
};

test "completion notice is sent to the job owner" {
    var outbox = std.ArrayList([]const u8).init(testing.allocator);
    defer outbox.deinit();
    var job = mkJob(1, 1, 1);
    job.owner = "ghost";
    const outcome = try notify.notifyDone(StubDirectory{}, &outbox, job);
    try testing.expectEqual(notify.Outcome.notified, outcome);
    try testing.expectEqual(@as(usize, 1), outbox.items.len);
}
