const std = @import("std");
const Config = @import("config.zig").Config;

/// One day of queue-wait measurements for one runner pool, as the CI metrics API
/// reports it. The API orders rows by day, oldest first.
pub const Sample = struct {
    record_id: []const u8,
    pool: []const u8,
    day: []const u8,
    wait_ms: i64,
};

/// One page of samples. `next_cursor` is null on the last page and carries the
/// token for the following page otherwise.
pub const Page = struct {
    samples: []Sample,
    next_cursor: ?[]const u8 = null,
};

/// Rows requested per page. The metrics API caps a page at 1000 rows.
pub const page_size: usize = 1000;

/// A reporting window for a busy fleet is a few hundred thousand samples, so the
/// collector refuses to work on anything larger than this.
pub const max_samples: usize = 250_000;

/// Upper bound on a single response body.
pub const max_response_bytes: usize = 64 * 1024 * 1024;

/// Fetches the queue-wait samples for the configured fleet.
pub fn fetchSamples(alloc: std.mem.Allocator, cfg: Config) ![]Sample {
    const path = "{s}/v1/queue_waits?fleet={s}&limit={d}";
    const url = try std.fmt.allocPrint(alloc, path, .{ cfg.metrics_url, cfg.fleet_id, page_size });

    var client = std.http.Client{ .allocator = alloc };
    defer client.deinit();

    var body = std.ArrayList(u8).init(alloc);
    const result = try client.fetch(.{
        .location = .{ .url = url },
        .method = .GET,
        .response_storage = .{ .dynamic = &body },
        .max_append_size = max_response_bytes,
    });
    if (result.status != .ok) return error.MetricsRequestFailed;

    const opts: std.json.ParseOptions = .{ .ignore_unknown_fields = true };
    const parsed = try std.json.parseFromSlice(Page, alloc, body.items, opts);
    const page = parsed.value;
    std.log.debug("metrics API returned {d} samples", .{page.samples.len});
    return page.samples;
}
