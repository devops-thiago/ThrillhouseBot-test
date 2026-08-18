const std = @import("std");
const policy = @import("policy.zig");

/// Freshness window applied to a dataset whose tier has no entry in
/// STALEGUARD_TIER_POLICY.
pub const default_window_hours: i64 = 24;

/// Where the status document is written when the deployment does not override it.
pub const default_status_path = "/var/lib/staleguard/status.json";

pub const Config = struct {
    /// Base URL of the backup catalog, without a trailing slash.
    catalog_url: []const u8,
    /// Estate whose backup jobs are swept.
    estate_id: []const u8,
    /// Per-tier freshness windows, in hours.
    tier_windows: []const policy.TierWindow,
    /// Window used for a tier that is not listed in `tier_windows`.
    default_window_hours: i64,
    /// Datasets that never appear in the summary.
    exempt_datasets: []const []const u8,
    /// File the status document is written to.
    status_path: []const u8,

    pub fn isExempt(self: Config, dataset: []const u8) bool {
        for (self.exempt_datasets) |exempt| {
            if (std.mem.eql(u8, exempt, dataset)) return true;
        }
        return false;
    }

    /// How old the last successful backup of a dataset in `tier` may get before
    /// the dataset is reported as stale.
    pub fn windowFor(self: Config, tier: []const u8) i64 {
        for (self.tier_windows) |window| {
            if (std.mem.eql(u8, window.tier, tier)) return window.hours;
        }
        return self.default_window_hours;
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

/// Reads the process environment into a Config. STALEGUARD_CATALOG_URL and
/// STALEGUARD_ESTATE_ID are required; everything else has a default.
pub fn load(alloc: std.mem.Allocator) !Config {
    const catalog_url = (try env(alloc, "STALEGUARD_CATALOG_URL")) orelse
        return error.MissingCatalogUrl;
    const estate_id = (try env(alloc, "STALEGUARD_ESTATE_ID")) orelse
        return error.MissingEstateId;

    var window_hours = default_window_hours;
    if (try env(alloc, "STALEGUARD_DEFAULT_WINDOW_HOURS")) |raw| {
        window_hours = try std.fmt.parseInt(i64, std.mem.trim(u8, raw, " \t"), 10);
        if (window_hours <= 0) return error.InvalidWindow;
    }

    var tier_windows: []const policy.TierWindow = &.{};
    if (try env(alloc, "STALEGUARD_TIER_POLICY")) |raw| {
        tier_windows = try policy.parse(alloc, raw);
    }

    var exempt_datasets: []const []const u8 = &.{};
    if (try env(alloc, "STALEGUARD_EXEMPT_DATASETS")) |raw| {
        exempt_datasets = try splitList(alloc, raw);
    }

    return .{
        .catalog_url = std.mem.trimRight(u8, catalog_url, "/"),
        .estate_id = estate_id,
        .tier_windows = tier_windows,
        .default_window_hours = window_hours,
        .exempt_datasets = exempt_datasets,
        .status_path = (try env(alloc, "STALEGUARD_STATUS_PATH")) orelse default_status_path,
    };
}
