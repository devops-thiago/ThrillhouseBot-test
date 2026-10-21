const std = @import("std");
const config = @import("config.zig");
const store = @import("store.zig");
const Compactor = @import("compactor.zig").Compactor;

pub fn main() !void {
    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const a = arena.allocator();

    const cfg = try config.load(a);
    var c = Compactor.init(a);
    defer c.deinit();

    const stdin = std.io.getStdIn().reader();
    while (try stdin.readUntilDelimiterOrEofAlloc(a, '\n', 4096)) |line| {
        try c.ingestLine(line);
    }

    const buckets = try c.compact();
    for (buckets) |b| {
        try store.saveBucket(a, cfg.db_path, b);
    }
    std.debug.print("buckets={d} skipped={d} suspect={d}\n", .{ buckets.len, c.skipped, c.suspect });
}
