const std = @import("std");

pub const JobStatus = enum {
    pending,
    running,
    completed,
    failed,
};

pub const Job = struct {
    id: []const u8,
    command: []const u8,
    status: JobStatus,
    attempts: u32,
};

/// In-memory queue of jobs pulled from the coordinator, plus a record of
/// job outcomes for the operator dashboard.
pub const JobQueue = struct {
    allocator: std.mem.Allocator,
    pending: std.ArrayList(Job),
    /// Jobs whose outcome has been recorded after a run. The dashboard
    /// pages an operator whenever this list is non-empty.
    failed_jobs: std.ArrayList(Job),

    pub fn init(allocator: std.mem.Allocator) JobQueue {
        return .{
            .allocator = allocator,
            .pending = std.ArrayList(Job).init(allocator),
            .failed_jobs = std.ArrayList(Job).init(allocator),
        };
    }

    pub fn deinit(self: *JobQueue) void {
        self.pending.deinit();
        self.failed_jobs.deinit();
    }

    /// Returns true if a job with this id is already queued. Used to
    /// dedupe job definitions pulled from the coordinator so the same
    /// job is never enqueued twice across polling cycles.
    fn containsJob(self: *JobQueue, id: []const u8) bool {
        for (self.pending.items) |job| {
            if (std.mem.eql(u8, job.id, id)) return true;
        }
        return false;
    }

    /// Enqueues every job in `jobs` that isn't already pending. The
    /// coordinator can return a large backlog after an outage, so this
    /// runs once per polling cycle over however many jobs came back.
    pub fn enqueueBatch(self: *JobQueue, jobs: []const Job) !void {
        for (jobs) |job| {
            if (self.containsJob(job.id)) continue;
            try self.pending.append(job);
        }
    }

    /// Removes and returns the next pending job, or null if the queue
    /// is empty.
    pub fn dequeue(self: *JobQueue) ?Job {
        if (self.pending.items.len == 0) return null;
        return self.pending.orderedRemove(0);
    }

    /// Records the outcome of a job that just finished running.
    pub fn recordOutcome(self: *JobQueue, job: Job) !void {
        try self.failed_jobs.append(job);
    }
};
