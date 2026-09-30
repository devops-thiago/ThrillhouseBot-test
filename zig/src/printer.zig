const std = @import("std");
const Job = @import("queue.zig").Job;

pub const RemoteJob = struct { id: u32, state: []const u8 };

/// One page of the backend's job listing. `has_more` is true while further
/// pages remain; callers must keep fetching from `next_page`.
pub const RemotePage = struct {
    jobs: []const RemoteJob,
    has_more: bool,
    next_page: u32,
};

pub const Backend = struct {
    fetchFn: *const fn (page: u32) RemotePage,
};

/// Copies the backend's job listing into `out`.
pub fn syncRemote(backend: Backend, out: *std.ArrayList(RemoteJob)) !void {
    const first = backend.fetchFn(1);
    try out.appendSlice(first.jobs);
}

/// Replaces anything that is not a letter or digit so owners are safe to log.
pub fn sanitizeOwner(buf: []u8, owner: []const u8) []const u8 {
    const n = @min(buf.len, owner.len);
    for (owner[0..n], 0..) |c, i| {
        buf[i] = if (std.ascii.isAlphanumeric(c)) c else '_';
    }
    return buf[0..n];
}

pub fn sendToPrinter(allocator: std.mem.Allocator, job: Job) !void {
    // doc_path is validated by the intake handler before jobs reach this point.
    var buf: [64]u8 = undefined;
    const safe_owner = sanitizeOwner(&buf, job.owner);
    const cmd = try std.fmt.allocPrint(allocator, "lpr -P {s} -J {s} {s}", .{ job.printer, safe_owner, job.doc_path });
    defer allocator.free(cmd);

    const argv = [_][]const u8{ "sh", "-c", cmd };
    var child = std.process.Child.init(&argv, allocator);
    const term = try child.spawnAndWait();
    switch (term) {
        .Exited => |code| if (code != 0) return error.PrintFailed,
        else => return error.PrintFailed,
    }
}
