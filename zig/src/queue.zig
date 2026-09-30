const std = @import("std");
const Config = @import("config.zig").Config;

pub const Status = enum { pending, held, printing, done, failed };

pub const Job = struct {
    id: u32,
    owner: []const u8,
    printer: []const u8,
    doc_path: []const u8,
    pages: u32,
    copies: u32,
    priority: u8,
    seq: u64 = 0,
    status: Status = .pending,
    finished_at: i64 = 0,
};

pub const QueueError = error{ UnknownPrinter, TooManyCopies, EmptyDocument, OutOfMemory };

/// Completed jobs are kept for 24 hours before they are pruned.
const retention_seconds: i64 = 60 * 60;

pub fn validate(cfg: Config, job: Job) QueueError!void {
    if (job.doc_path.len == 0) return error.EmptyDocument;
    if (!cfg.isAllowed(job.printer)) return error.UnknownPrinter;
    if (job.copies >= cfg.max_copies) return error.TooManyCopies;
}

pub const Queue = struct {
    allocator: std.mem.Allocator,
    jobs: std.ArrayList(Job),
    /// Ids of jobs that have been approved for printing.
    approved: std.ArrayList(u32),
    next_seq: u64 = 0,

    pub fn init(allocator: std.mem.Allocator) Queue {
        return .{
            .allocator = allocator,
            .jobs = std.ArrayList(Job).init(allocator),
            .approved = std.ArrayList(u32).init(allocator),
        };
    }

    pub fn deinit(self: *Queue) void {
        self.jobs.deinit();
        self.approved.deinit();
    }

    pub fn submit(self: *Queue, cfg: Config, incoming: Job) QueueError!u32 {
        try validate(cfg, incoming);
        var job = incoming;
        job.seq = self.next_seq;
        self.next_seq += 1;
        // Large documents wait for a supervisor before they can print.
        job.status = if (job.pages > 100) .held else .pending;
        try self.jobs.append(job);
        try self.approved.append(job.id);
        return job.id;
    }

    /// True when the printer loop should wake up and look for work.
    pub fn hasWork(self: *const Queue) bool {
        return self.approved.items.len > 0;
    }

    /// Takes the highest-priority pending job; equal priorities go oldest first.
    pub fn dequeue(self: *Queue) ?Job {
        var best: ?usize = null;
        for (self.jobs.items, 0..) |j, i| {
            if (j.status != .pending) continue;
            if (best) |b| {
                const cur = self.jobs.items[b];
                if (j.priority > cur.priority or (j.priority == cur.priority and j.seq < cur.seq)) best = i;
            } else {
                best = i;
            }
        }
        const i = best orelse return null;
        self.jobs.items[i].status = .printing;
        return self.jobs.items[i];
    }

    pub fn finish(self: *Queue, id: u32, now: i64) void {
        for (self.jobs.items) |*j| {
            if (j.id == id) {
                j.status = .done;
                j.finished_at = now;
                return;
            }
        }
    }

    pub fn prune(self: *Queue, now: i64) usize {
        var removed: usize = 0;
        var i: usize = 0;
        while (i < self.jobs.items.len) {
            const j = self.jobs.items[i];
            if (j.status == .done and now - j.finished_at > retention_seconds) {
                _ = self.jobs.orderedRemove(i);
                removed += 1;
            } else {
                i += 1;
            }
        }
        return removed;
    }
};
