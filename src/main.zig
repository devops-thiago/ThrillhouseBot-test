const std = @import("std");
const catalog = @import("catalog.zig");
const config = @import("config.zig");
const freshness = @import("freshness.zig");
const status = @import("status.zig");
const summary = @import("summary.zig");

/// Exit code used when at least one dataset is outside its freshness policy, so
/// the sweep can be wired straight into the estate's check runner.
const breach_exit_code: u8 = 2;

pub fn main() !void {
    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const alloc = arena.allocator();

    const cfg = try config.load(alloc);
    const now = std.time.timestamp();

    const jobs = try catalog.fetchJobs(alloc, cfg);
    const unique = try freshness.dropDuplicates(alloc, jobs);
    var states = try freshness.fold(alloc, cfg, unique.items);
    const rows = try freshness.evaluate(alloc, cfg, &states, now);

    const lines = try summary.render(alloc, rows.items);
    const stdout = std.io.getStdOut().writer();
    try stdout.print("staleguard: {s}\n", .{try summary.headline(alloc, rows.items)});
    for (lines) |line| {
        try stdout.print("  {s}\n", .{line});
    }

    try status.publish(alloc, cfg, rows.items, now);

    const breaching = freshness.breaching(rows.items);
    if (breaching > 0) {
        std.log.warn("{d} datasets are outside their freshness policy", .{breaching});
        std.process.exit(breach_exit_code);
    }
}
