const std = @import("std");
const testing = std.testing;
const coordinator_mod = @import("../src/coordinator.zig");
const queue = @import("../src/queue.zig");

/// Stub fetch used in place of the real network call. Always returns the
/// same single-page payload, regardless of the URL (and therefore the
/// page number) requested.
fn stubFetch(allocator: std.mem.Allocator, url: []const u8) anyerror![]const u8 {
    _ = url;
    return allocator.dupe(u8,
        \\{"jobs":[{"id":"job-1","command":"echo hi"}],"has_more":false,"next_page":0}
    );
}

test "fetchPendingJobs returns the jobs reported by the coordinator" {
    const allocator = testing.allocator;
    var coordinator = coordinator_mod.Coordinator.init(allocator, "https://coordinator.internal", stubFetch);

    const jobs = try coordinator.fetchPendingJobs();
    defer {
        for (jobs) |job| {
            allocator.free(job.id);
            allocator.free(job.command);
        }
        allocator.free(jobs);
    }

    try testing.expectEqual(@as(usize, 1), jobs.len);
    try testing.expectEqualStrings("job-1", jobs[0].id);
}

test "JobQueue.enqueueBatch skips jobs already pending" {
    const allocator = testing.allocator;
    var q = queue.JobQueue.init(allocator);
    defer q.deinit();

    const batch = [_]queue.Job{
        .{ .id = "a", .command = "true", .status = .pending, .attempts = 0 },
        .{ .id = "b", .command = "true", .status = .pending, .attempts = 0 },
    };
    try q.enqueueBatch(&batch);
    try q.enqueueBatch(&batch);

    try testing.expectEqual(@as(usize, 2), q.pending.items.len);
}

test "JobQueue.dequeue returns null once empty" {
    const allocator = testing.allocator;
    var q = queue.JobQueue.init(allocator);
    defer q.deinit();

    const batch = [_]queue.Job{
        .{ .id = "a", .command = "true", .status = .pending, .attempts = 0 },
    };
    try q.enqueueBatch(&batch);

    _ = q.dequeue();
    try testing.expectEqual(@as(?queue.Job, null), q.dequeue());
}
