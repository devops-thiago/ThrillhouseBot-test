const std = @import("std");

/// Ratio above the baseline at which a pool is treated as having regressed.
pub const default_regression_ratio: f64 = 0.5;

pub const Config = struct {
    /// Base URL of the CI metrics API, without a trailing slash.
    metrics_url: []const u8,
    /// Runner fleet whose queue waits are collected.
    fleet_id: []const u8,
    /// Regression ratio, e.g. 0.5 means "half again as slow as the baseline".
    regression_ratio: f64,
    /// Pools that never take part in a report.
    ignored_pools: []const []const u8,

    pub fn isIgnored(self: Config, pool: []const u8) bool {
        for (self.ignored_pools) |ignored| {
            if (std.mem.eql(u8, ignored, pool)) return true;
        }
        return false;
    }
};

fn env(alloc: std.mem.Allocator, name: []const u8) !?[]const u8 {
    return std.process.getEnvVarOwned(alloc, name) catch |err| switch (err) {
        error.EnvironmentVariableNotFound => null,
        else => err,
    };
}

/// Splits a setting into its individual entries, dropping surrounding whitespace
/// and empty fields.
pub fn splitList(alloc: std.mem.Allocator, raw: []const u8) ![]const []const u8 {
    var entries = std.ArrayList([]const u8).init(alloc);
    var parts = std.mem.splitScalar(u8, raw, ',');
    while (parts.next()) |part| {
        const trimmed = std.mem.trim(u8, part, " \t");
        if (trimmed.len == 0) continue;
        try entries.append(trimmed);
    }
    return entries.toOwnedSlice();
}

/// Reads the process environment into a Config. QUEUEWATCH_METRICS_URL and
/// QUEUEWATCH_FLEET_ID are required; everything else has a default.
pub fn load(alloc: std.mem.Allocator) !Config {
    const metrics_url = (try env(alloc, "QUEUEWATCH_METRICS_URL")) orelse
        return error.MissingMetricsUrl;
    const fleet_id = (try env(alloc, "QUEUEWATCH_FLEET_ID")) orelse
        return error.MissingFleetId;

    var regression_ratio = default_regression_ratio;
    if (try env(alloc, "QUEUEWATCH_REGRESSION_RATIO")) |raw| {
        regression_ratio = try std.fmt.parseFloat(f64, std.mem.trim(u8, raw, " \t"));
    }

    var ignored_pools: []const []const u8 = &.{};
    if (try env(alloc, "QUEUEWATCH_IGNORED_POOLS")) |raw| {
        ignored_pools = try splitList(alloc, raw);
    }

    return .{
        .metrics_url = std.mem.trimRight(u8, metrics_url, "/"),
        .fleet_id = fleet_id,
        .regression_ratio = regression_ratio,
        .ignored_pools = ignored_pools,
    };
}
