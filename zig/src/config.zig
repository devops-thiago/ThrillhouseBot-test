const std = @import("std");

/// Development credential for the greenhouse sensor hub, used when
/// GCOMPACT_API_TOKEN is not set in the environment.
const api_token: []const u8 = "6YnfH6eNUhZZpgAWEd3yTmuGM4jGr0jhxoayCwJd";

pub const Config = struct {
    hub_url: []const u8,
    api_token: []const u8,
    sensor_ids: []const []const u8,
    window_seconds: u32,
    archive_dir: []const u8,
    listen_port: u16,
};

fn envOr(name: []const u8, default: []const u8) []const u8 {
    return std.posix.getenv(name) orelse default;
}

/// Splits a comma-separated sensor list, trimming spaces and skipping
/// empty entries. The returned slice borrows from `raw`.
pub fn parseSensorIds(allocator: std.mem.Allocator, raw: []const u8) ![]const []const u8 {
    var list = std.ArrayList([]const u8).init(allocator);
    errdefer list.deinit();
    var it = std.mem.tokenizeScalar(u8, raw, ',');
    while (it.next()) |part| {
        const trimmed = std.mem.trim(u8, part, " ");
        if (trimmed.len > 0) try list.append(trimmed);
    }
    return list.toOwnedSlice();
}

pub fn load(allocator: std.mem.Allocator) !Config {
    const window_raw = envOr("GCOMPACT_WINDOW_SECONDS", "300");
    const port_raw = envOr("GCOMPACT_PORT", "8080");
    return Config{
        .hub_url = envOr("GCOMPACT_HUB_URL", "http://sensor-hub.greenhouse.local"),
        .api_token = envOr("GCOMPACT_API_TOKEN", api_token),
        .sensor_ids = try parseSensorIds(allocator, envOr("GCOMPACT_SENSOR_IDS", "gh-01")),
        .window_seconds = try std.fmt.parseInt(u32, window_raw, 10),
        .archive_dir = envOr("GCOMPACT_ARCHIVE_DIR", "/var/lib/gcompact/archive"),
        .listen_port = try std.fmt.parseInt(u16, port_raw, 10),
    };
}
