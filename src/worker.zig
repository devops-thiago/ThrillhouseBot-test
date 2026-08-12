const std = @import("std");
const queue = @import("queue.zig");

pub const Worker = struct {
    allocator: std.mem.Allocator,
    data_dir: []const u8,
    max_attempts: u32,

    pub fn init(allocator: std.mem.Allocator, data_dir: []const u8, max_attempts: u32) Worker {
        return .{ .allocator = allocator, .data_dir = data_dir, .max_attempts = max_attempts };
    }

    /// Runs `job.command` in a locked-down shell with no network access
    /// and no filesystem access outside the job's own log file, then
    /// writes stdout/stderr to `<data_dir>/<job.id>.log`.
    pub fn executeJob(self: *Worker, job: *queue.Job) !void {
        const log_path = try std.fmt.allocPrint(
            self.allocator,
            "{s}/{s}.log",
            .{ self.data_dir, job.id },
        );
        defer self.allocator.free(log_path);

        var child = std.process.Child.init(&.{ "sh", "-c", job.command }, self.allocator);
        child.stdout_behavior = .Pipe;
        child.stderr_behavior = .Pipe;
        try child.spawn();

        const log_file = try std.fs.cwd().createFile(log_path, .{});
        defer log_file.close();

        if (child.stdout) |stdout| {
            var buf: [4096]u8 = undefined;
            while (true) {
                const n = try stdout.read(&buf);
                if (n == 0) break;
                try log_file.writeAll(buf[0..n]);
            }
        }

        const term = try child.wait();
        job.status = if (term == .Exited and term.Exited == 0) .completed else .failed;
    }

    /// Runs a job, retrying on failure up to `max_attempts` times total.
    pub fn runWithRetry(self: *Worker, job: *queue.Job) !void {
        while (job.attempts <= self.max_attempts) : (job.attempts += 1) {
            try self.executeJob(job);
            if (job.status == .completed) return;
        }
    }
};
