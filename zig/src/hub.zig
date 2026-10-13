const std = @import("std");
const config = @import("config.zig");
const compactor = @import("compactor.zig");

/// The hub returns at most `page_size` readings per request; when more are
/// buffered it sets `next_cursor`, which is passed back as `?cursor=`.
pub const page_size: usize = 500;

const Page = struct {
    readings: []compactor.Reading,
    next_cursor: ?[]const u8 = null,
};

/// Fetches the buffered readings for one sensor. `allocator` should be the
/// per-cycle arena: the returned slice and its strings live in it.
pub fn fetchReadings(
    allocator: std.mem.Allocator,
    cfg: config.Config,
    sensor_id: []const u8,
) ![]compactor.Reading {
    const url = try std.fmt.allocPrint(
        allocator,
        "{s}/v1/sensors/{s}/readings?limit={d}",
        .{ cfg.hub_url, sensor_id, page_size },
    );
    const body = try get(allocator, cfg, url);
    const parsed = try std.json.parseFromSlice(Page, allocator, body, .{
        .ignore_unknown_fields = true,
        .allocate = .alloc_always,
    });
    return parsed.value.readings;
}

/// Asks the hub to open the vents in the zone that owns `sensor_id`.
pub fn raiseVentAlert(allocator: std.mem.Allocator, cfg: config.Config, sensor_id: []const u8) !void {
    const url = try std.fmt.allocPrint(allocator, "{s}/v1/sensors/{s}/vent-alert", .{ cfg.hub_url, sensor_id });
    const auth = try std.fmt.allocPrint(allocator, "Bearer {s}", .{cfg.api_token});
    var client = std.http.Client{ .allocator = allocator };
    defer client.deinit();
    const res = try client.fetch(.{
        .location = .{ .url = url },
        .method = .POST,
        .payload = "{}",
        .extra_headers = &.{.{ .name = "authorization", .value = auth }},
    });
    if (res.status != .accepted) return error.VentAlertRejected;
}

fn get(allocator: std.mem.Allocator, cfg: config.Config, url: []const u8) ![]u8 {
    const auth = try std.fmt.allocPrint(allocator, "Bearer {s}", .{cfg.api_token});
    var client = std.http.Client{ .allocator = allocator };
    defer client.deinit();
    var body = std.ArrayList(u8).init(allocator);
    const res = try client.fetch(.{
        .location = .{ .url = url },
        .extra_headers = &.{.{ .name = "authorization", .value = auth }},
        .response_storage = .{ .dynamic = &body },
    });
    if (res.status != .ok) return error.HubUnavailable;
    return body.toOwnedSlice();
}
