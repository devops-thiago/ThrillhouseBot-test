const std = @import("std");
const config = @import("config.zig");
const queue = @import("queue.zig");
const manifest = @import("manifest.zig");
const scanner = @import("scanner.zig");

/// Returns the raw value of `name` from the query string of `target`.
fn queryParam(target: []const u8, name: []const u8) ?[]const u8 {
    const q = std.mem.indexOfScalar(u8, target, '?') orelse return null;
    var it = std.mem.splitScalar(u8, target[q + 1 ..], '&');
    while (it.next()) |pair| {
        const eq = std.mem.indexOfScalar(u8, pair, '=') orelse continue;
        if (std.mem.eql(u8, pair[0..eq], name)) return pair[eq + 1 ..];
    }
    return null;
}

fn handle(allocator: std.mem.Allocator, scan_alloc: std.mem.Allocator, cfg: config.Config, q: *queue.SampleQueue, req: *std.http.Server.Request) !void {
    const target = req.head.target;
    if (std.mem.startsWith(u8, target, "/scan")) {
        const codes = queryParam(target, "codes") orelse return req.respond("missing codes\n", .{ .status = .bad_request });
        const owned = try scan_alloc.dupe(u8, codes);
        var wells = [_]?[]const u8{null} ** 96;
        var it = std.mem.splitScalar(u8, owned, ',');
        var i: usize = 0;
        while (it.next()) |code| : (i += 1) {
            if (i == wells.len) break;
            wells[i] = if (code.len == 0) null else code;
        }
        const n = try scanner.ingestTray(scan_alloc, scanner.TrayScanner{ .wells = &wells }, q, std.time.timestamp());
        var buf: [32]u8 = undefined;
        return req.respond(try std.fmt.bufPrint(&buf, "queued={d}\n", .{n}), .{});
    }
    if (std.mem.startsWith(u8, target, "/export")) {
        const rack = queryParam(target, "rack") orelse return req.respond("missing rack\n", .{ .status = .bad_request });
        const operator = queryParam(target, "operator") orelse "unknown";
        const path = manifest.writeManifest(allocator, cfg.export_dir, rack, operator, q.samples.items) catch
            return req.respond("export failed\n", .{ .status = .internal_server_error });
        defer allocator.free(path);
        return req.respond(path, .{});
    }
    if (std.mem.startsWith(u8, target, "/status")) {
        var buf: [128]u8 = undefined;
        const body = try std.fmt.bufPrint(&buf, "queued={d} rejected={d} stale={d} ready={}\n", .{
            q.len(), q.rejected, q.countStale(std.time.timestamp(), cfg.stale_after_min), q.readyForRun(),
        });
        return req.respond(body, .{});
    }
    return req.respond("not found\n", .{ .status = .not_found });
}

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    var env = try std.process.getEnvMap(allocator);
    defer env.deinit();
    const cfg = try config.load(allocator, &env);
    defer allocator.free(cfg.allowed_sites);

    var q = queue.SampleQueue.init(allocator, queue.max_shift_samples);
    defer q.deinit();
    // Scanned barcodes are referenced by the queue for the life of the process.
    var scan_arena = std.heap.ArenaAllocator.init(allocator);
    defer scan_arena.deinit();

    const address = try std.net.Address.parseIp("0.0.0.0", 8080);
    var listener = try address.listen(.{ .reuse_address = true });
    defer listener.deinit();
    std.log.info("labq listening on :8080, export dir {s}", .{cfg.export_dir});

    while (true) {
        const conn = try listener.accept();
        defer conn.stream.close();
        var read_buf: [8192]u8 = undefined;
        var server = std.http.Server.init(conn, &read_buf);
        var req = server.receiveHead() catch continue;
        handle(allocator, scan_arena.allocator(), cfg, &q, &req) catch |err| std.log.err("request failed: {}", .{err});
    }
}
