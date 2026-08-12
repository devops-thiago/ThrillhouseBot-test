const std = @import("std");
const queue = @import("queue.zig");

const jobs_per_page: u32 = 50;

/// One page of the coordinator's `/jobs` listing.
pub const JobsPage = struct {
    jobs: []queue.Job,
    has_more: bool,
    next_page: u32,
};

/// A fetch function that performs a GET against `url` and returns the raw
/// response body. Swappable so tests can stub the network.
pub const FetchFn = *const fn (allocator: std.mem.Allocator, url: []const u8) anyerror![]const u8;

pub const Coordinator = struct {
    allocator: std.mem.Allocator,
    base_url: []const u8,
    fetch: FetchFn,

    pub fn init(allocator: std.mem.Allocator, base_url: []const u8, fetch: FetchFn) Coordinator {
        return .{ .allocator = allocator, .base_url = base_url, .fetch = fetch };
    }

    /// Fetches the coordinator's pending-job listing and returns the jobs
    /// ready to run.
    pub fn fetchPendingJobs(self: *Coordinator) ![]queue.Job {
        const page = try self.fetchPage(1);
        return page.jobs;
    }

    fn fetchPage(self: *Coordinator, page_num: u32) !JobsPage {
        const url = try std.fmt.allocPrint(
            self.allocator,
            "{s}/jobs?page={d}&per_page={d}",
            .{ self.base_url, page_num, jobs_per_page },
        );
        defer self.allocator.free(url);

        const body = try self.fetch(self.allocator, url);
        defer self.allocator.free(body);

        return parsePage(self.allocator, body);
    }

    fn parsePage(allocator: std.mem.Allocator, body: []const u8) !JobsPage {
        const parsed = try std.json.parseFromSlice(RawPage, allocator, body, .{});
        defer parsed.deinit();

        var jobs = try allocator.alloc(queue.Job, parsed.value.jobs.len);
        for (parsed.value.jobs, 0..) |raw, i| {
            jobs[i] = .{
                .id = try allocator.dupe(u8, raw.id),
                .command = try allocator.dupe(u8, raw.command),
                .status = .pending,
                .attempts = 0,
            };
        }

        return JobsPage{
            .jobs = jobs,
            .has_more = parsed.value.has_more,
            .next_page = parsed.value.next_page,
        };
    }
};

const RawJob = struct {
    id: []const u8,
    command: []const u8,
};

const RawPage = struct {
    jobs: []RawJob,
    has_more: bool,
    next_page: u32,
};

/// Real network fetch used in production. Returns `error.CoordinatorHttpError`
/// for any non-200 response.
pub fn httpFetch(allocator: std.mem.Allocator, url: []const u8) ![]const u8 {
    var client = std.http.Client{ .allocator = allocator };
    defer client.deinit();

    var body = std.ArrayList(u8).init(allocator);
    errdefer body.deinit();

    const result = try client.fetch(.{
        .location = .{ .url = url },
        .response_storage = .{ .dynamic = &body },
    });

    if (result.status != .ok) return error.CoordinatorHttpError;
    return body.toOwnedSlice();
}
