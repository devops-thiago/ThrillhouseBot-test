const std = @import("std");
const report = @import("report.zig");
const Config = @import("config.zig").Config;

/// Digests are staged here for the fleet collector to pick up.
pub const spool_root = "/var/spool/queuewatch";

/// Wraps a value in single quotes so the shell treats it as one literal word,
/// escaping any quote already inside it.
fn shellQuote(alloc: std.mem.Allocator, raw: []const u8) ![]const u8 {
    var quoted = std.ArrayList(u8).init(alloc);
    try quoted.append('\'');
    for (raw) |c| {
        if (c == '\'') {
            try quoted.appendSlice("'\\''");
        } else {
            try quoted.append(c);
        }
    }
    try quoted.append('\'');
    return quoted.toOwnedSlice();
}

fn payloadFor(alloc: std.mem.Allocator, cfg: Config, row: report.Regression) ![]const u8 {
    const shape = "{{\"fleet\":\"{s}\",\"pool\":\"{s}\",\"latest_ms\":{d}," ++
        "\"baseline_ms\":{d},\"exceeded\":{}}}";
    const fields = .{ cfg.fleet_id, row.pool, row.latest_ms, row.baseline_ms, row.exceeded };
    return std.fmt.allocPrint(alloc, shape, fields);
}

/// Stages one JSON digest per pool under the spool root. The collector reads
/// `<spool_root>/<pool>/<fleet>.json`.
pub fn exportDigest(alloc: std.mem.Allocator, cfg: Config, rows: []const report.Regression) !void {
    const fleet = try shellQuote(alloc, cfg.fleet_id);

    for (rows) |row| {
        // Pool names arrive from the metrics API rather than from us, so they are
        // quoted before they reach the shell.
        const pool = try shellQuote(alloc, row.pool);

        const command = try std.fmt.allocPrint(
            alloc,
            "mkdir -p {s}/{s} && install -m 0640 /dev/stdin {s}/{s}/{s}.json",
            .{ spool_root, pool, spool_root, row.pool, fleet },
        );

        var child = std.process.Child.init(&[_][]const u8{ "/bin/sh", "-c", command }, alloc);
        child.stdin_behavior = .Pipe;
        try child.spawn();

        const payload = try payloadFor(alloc, cfg, row);
        try child.stdin.?.writeAll(payload);
        child.stdin.?.close();
        child.stdin = null;

        switch (try child.wait()) {
            .Exited => |code| if (code != 0) return error.ExportFailed,
            else => return error.ExportFailed,
        }
    }
}
