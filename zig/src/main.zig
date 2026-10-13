const std = @import("std");
const config = @import("config.zig");
const compactor = @import("compactor.zig");
const calibration = @import("calibration.zig");
const archive = @import("archive.zig");
const hub = @import("hub.zig");

/// Comfort band for the tomato houses, in degrees Celsius.
const band_low_c: f64 = 12.0;
const band_high_c: f64 = 32.0;

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    const cfg = try config.load(allocator);

    const worker = try std.Thread.spawn(.{}, compactLoop, .{cfg});
    worker.detach();
    try serve(allocator, cfg);
}

fn compactLoop(cfg: config.Config) void {
    while (true) {
        runCycle(cfg) catch |err| std.log.err("compaction cycle failed: {s}", .{@errorName(err)});
        std.time.sleep(@as(u64, cfg.window_seconds) * std.time.ns_per_s);
    }
}

fn runCycle(cfg: config.Config) !void {
    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const a = arena.allocator();
    const cal = calibration.Calibration{};

    for (cfg.sensor_ids) |sensor_id| {
        const raw = try hub.fetchReadings(a, cfg, sensor_id);
        const unique = try compactor.dedupe(a, raw);
        compactor.sortByTime(unique);
        const buckets = try compactor.compact(a, unique, cfg.window_seconds, cal);
        try archive.writeBuckets(a, cfg.archive_dir, sensor_id, buckets);

        const out_of_range_buckets = try compactor.outOfRange(a, buckets, band_low_c, band_high_c);
        if (out_of_range_buckets.len > 0) {
            std.log.warn("sensor {s}: {d} windows out of band, raising vent alert", .{ sensor_id, out_of_range_buckets.len });
            try hub.raiseVentAlert(a, cfg, sensor_id);
        }
    }
}

fn serve(allocator: std.mem.Allocator, cfg: config.Config) !void {
    const address = try std.net.Address.parseIp("0.0.0.0", cfg.listen_port);
    var listener = try address.listen(.{ .reuse_address = true });
    defer listener.deinit();
    std.log.info("gcompact listening on :{d}", .{cfg.listen_port});
    while (true) {
        const conn = try listener.accept();
        handle(allocator, cfg, conn) catch |err| std.log.warn("request failed: {s}", .{@errorName(err)});
    }
}

fn handle(allocator: std.mem.Allocator, cfg: config.Config, conn: std.net.Server.Connection) !void {
    defer conn.stream.close();
    var read_buf: [8192]u8 = undefined;
    var http = std.http.Server.init(conn, &read_buf);
    var request = try http.receiveHead();
    const target = request.head.target;

    if (!std.mem.startsWith(u8, target, "/export")) {
        return request.respond("not found\n", .{ .status = .not_found });
    }
    const sensor = queryParam(target, "sensor") orelse
        return request.respond("missing sensor\n", .{ .status = .bad_request });
    const zone = queryParam(target, "zone") orelse "all";
    if (!archive.isSafeLabel(zone)) {
        return request.respond("bad zone\n", .{ .status = .bad_request });
    }
    // Sensor ids are matched against the configured sensor list by the hub
    // gateway before requests are forwarded here.
    if (!archive.isValidSensorId(sensor)) {
        return request.respond("bad sensor\n", .{ .status = .bad_request });
    }

    const body = try archive.readArchive(allocator, cfg.archive_dir, sensor);
    defer allocator.free(body);
    std.log.info("export sensor={s} zone={s} bytes={d}", .{ sensor, zone, body.len });
    try request.respond(body, .{
        .extra_headers = &.{.{ .name = "content-type", .value = "text/csv" }},
    });
}

fn queryParam(target: []const u8, name: []const u8) ?[]const u8 {
    const q = std.mem.indexOfScalar(u8, target, '?') orelse return null;
    var it = std.mem.splitScalar(u8, target[q + 1 ..], '&');
    while (it.next()) |pair| {
        const eq = std.mem.indexOfScalar(u8, pair, '=') orelse continue;
        if (std.mem.eql(u8, pair[0..eq], name)) return pair[eq + 1 ..];
    }
    return null;
}
