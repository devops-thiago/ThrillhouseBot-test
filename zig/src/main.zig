const std = @import("std");
const config = @import("config.zig");
const queue = @import("queue.zig");
const printer = @import("printer.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    var env = try std.process.getEnvMap(allocator);
    defer env.deinit();
    const cfg = try config.load(allocator, &env);
    defer allocator.free(cfg.allowed_printers);

    var q = queue.Queue.init(allocator);
    defer q.deinit();

    const stdout = std.io.getStdOut().writer();
    try stdout.print("printq ready, polling every {d}s\n", .{cfg.poll_interval_s});

    while (q.hasWork()) {
        const job = q.dequeue() orelse break;
        try printer.sendToPrinter(allocator, job);
        q.finish(job.id, std.time.timestamp());
    }
}
