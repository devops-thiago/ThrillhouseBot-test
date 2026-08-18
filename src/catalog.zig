const std = @import("std");
const Config = @import("config.zig").Config;

/// One backup job as the catalog reports it. `finished_at` is a Unix timestamp
/// in seconds; jobs that are still running are not part of the response.
pub const Job = struct {
    job_id: []const u8,
    dataset: []const u8,
    tier: []const u8,
    /// "succeeded", "failed" or "cancelled".
    outcome: []const u8,
    finished_at: i64,
    bytes_written: u64,
};

/// One page of jobs. `next_cursor` carries the token for the following page and
/// is null on the last one.
pub const Page = struct {
    jobs: []Job,
    next_cursor: ?[]const u8 = null,
};

/// Jobs requested per page. The catalog caps a page at 500 rows.
pub const page_size: usize = 500;

/// A nightly estate produces a few thousand job records per sweep window; well
/// past this and something is wrong with the query rather than the estate.
pub const max_jobs: usize = 100_000;

/// Upper bound on a single response body.
pub const max_response_bytes: usize = 32 * 1024 * 1024;

/// True when the job wrote a usable restore point. Cancelled and failed runs
/// still show up in the sweep, they just do not refresh a dataset.
pub fn succeeded(job: Job) bool {
    return std.mem.eql(u8, job.outcome, "succeeded");
}

/// Fetches the backup jobs recorded for the configured estate in the catalog's
/// current retention window.
pub fn fetchJobs(alloc: std.mem.Allocator, cfg: Config) ![]Job {
    const path = "{s}/v1/backup_jobs?estate={s}&limit={d}";
    const url = try std.fmt.allocPrint(alloc, path, .{ cfg.catalog_url, cfg.estate_id, page_size });

    var client = std.http.Client{ .allocator = alloc };
    defer client.deinit();

    var body = std.ArrayList(u8).init(alloc);
    const result = try client.fetch(.{
        .location = .{ .url = url },
        .method = .GET,
        .response_storage = .{ .dynamic = &body },
        .max_append_size = max_response_bytes,
    });
    if (result.status != .ok) return error.CatalogRequestFailed;

    const opts: std.json.ParseOptions = .{ .ignore_unknown_fields = true };
    const parsed = try std.json.parseFromSlice(Page, alloc, body.items, opts);
    const page = parsed.value;
    if (page.jobs.len > max_jobs) return error.TooManyJobs;

    std.log.debug("catalog returned {d} job records", .{page.jobs.len});
    return page.jobs;
}
