const std = @import("std");
const config = @import("config.zig");
const digest = @import("digest.zig");
const exporter = @import("exporter.zig");
const limiter = @import("limiter.zig");
const metrics = @import("metrics.zig");
const notifier = @import("notifier.zig");
const report = @import("report.zig");

/// Alert lines the channel accepts in one run.
const alert_burst: u32 = 25;

pub fn main() !void {
    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const cfg = try config.load(alloc);

    const samples = try metrics.fetchSamples(alloc, cfg);
    const unique = try report.dropDuplicates(alloc, samples);
    var totals = try report.ingest(alloc, cfg, unique.items);
    const slow_pools = try report.slowPools(alloc, cfg, &totals);

    if (slow_pools.items.len == 0) {
        std.log.info("every pool is queueing within its baseline, nothing to alert on", .{});
        return;
    }

    const lines = try digest.render(alloc, slow_pools.items);
    var bucket = limiter.TokenBucket.init(alert_burst);
    const stdout = std.io.getStdOut().writer().any();
    const sent = try notifier.sendAll(&bucket, stdout, lines);
    if (sent < lines.len) {
        std.log.warn("{d} alert lines deferred to the next run", .{lines.len - sent});
    }

    try exporter.exportDigest(alloc, cfg, slow_pools.items);
}
