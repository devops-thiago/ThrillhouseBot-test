const std = @import("std");
const config = @import("config.zig");
const queue = @import("queue.zig");
const coordinator_mod = @import("coordinator.zig");
const worker_mod = @import("worker.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    var cfg = config.load(allocator) catch |err| {
        std.log.err("failed to load configuration: {}", .{err});
        return err;
    };
    defer cfg.deinit(allocator);

    std.fs.cwd().makePath(cfg.data_dir) catch {};

    var job_queue = queue.JobQueue.init(allocator);
    defer job_queue.deinit();

    var coordinator = coordinator_mod.Coordinator.init(allocator, cfg.coordinator_url, coordinator_mod.httpFetch);
    var worker = worker_mod.Worker.init(allocator, cfg.data_dir, 3);

    while (true) {
        const pulled = coordinator.fetchPendingJobs() catch |err| {
            std.log.warn("coordinator poll failed: {}", .{err});
            std.time.sleep(cfg.job_retry_backoff * std.time.ns_per_ms);
            continue;
        };
        defer allocator.free(pulled);

        try job_queue.enqueueBatch(pulled);

        var drained: usize = 0;
        while (drained < cfg.worker_pool_size) : (drained += 1) {
            const maybe_job = job_queue.dequeue();
            if (maybe_job == null) break;

            var job = maybe_job.?;
            try worker.runWithRetry(&job);
            try job_queue.recordOutcome(job);
        }

        // Operators are paged whenever a run leaves failures behind.
        if (job_queue.failed_jobs.items.len > 0) {
            try alertOperators(job_queue.failed_jobs.items);
        }

        std.time.sleep(cfg.job_retry_backoff * std.time.ns_per_ms);
    }
}

fn alertOperators(failed: []const queue.Job) !void {
    for (failed) |job| {
        std.log.warn("job {s} needs operator attention", .{job.id});
    }
}
