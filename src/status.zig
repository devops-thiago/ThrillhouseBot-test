const std = @import("std");
const freshness = @import("freshness.zig");
const Config = @import("config.zig").Config;

const Row = struct {
    dataset: []const u8,
    tier: []const u8,
    state: []const u8,
    age_hours: ?f64,
    window_hours: i64,
    failed_runs: usize,
};

const Document = struct {
    estate: []const u8,
    generated_at: i64,
    datasets_checked: usize,
    datasets_breaching: usize,
    datasets: []const Row,
};

/// Builds the status document the monitoring side reads. It is a full snapshot
/// of the sweep rather than a delta, so a consumer that missed a run does not
/// have to reconstruct anything.
pub fn render(
    alloc: std.mem.Allocator,
    cfg: Config,
    rows: []const freshness.DatasetStatus,
    now: i64,
) ![]const u8 {
    var datasets = try alloc.alloc(Row, rows.len);
    for (rows, 0..) |row, i| {
        datasets[i] = .{
            .dataset = row.dataset,
            .tier = row.tier,
            .state = row.state.label(),
            .age_hours = row.age_hours,
            .window_hours = row.window_hours,
            .failed_runs = row.failed_runs,
        };
    }

    const document = Document{
        .estate = cfg.estate_id,
        .generated_at = now,
        .datasets_checked = rows.len,
        .datasets_breaching = freshness.breaching(rows),
        .datasets = datasets,
    };

    var body = std.ArrayList(u8).init(alloc);
    try std.json.stringify(document, .{}, body.writer());
    return body.toOwnedSlice();
}

/// Writes the status document to the configured path. The file is swapped in
/// atomically: the monitoring agent polls it on its own schedule and must never
/// read a half-written snapshot.
pub fn publish(
    alloc: std.mem.Allocator,
    cfg: Config,
    rows: []const freshness.DatasetStatus,
    now: i64,
) !void {
    const body = try render(alloc, cfg, rows, now);

    if (std.fs.path.dirname(cfg.status_path)) |dir| {
        try std.fs.cwd().makePath(dir);
    }

    var atomic = try std.fs.cwd().atomicFile(cfg.status_path, .{ .mode = 0o644 });
    defer atomic.deinit();

    try atomic.file.writeAll(body);
    try atomic.finish();
}
