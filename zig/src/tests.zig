const std = @import("std");
const barcode = @import("barcode.zig");
const config = @import("config.zig");
const queue = @import("queue.zig");
const scanner = @import("scanner.zig");
const lims = @import("lims.zig");
const manifest = @import("manifest.zig");

const testing = std.testing;

fn mkSample(code: []const u8, at: i64) queue.Sample {
    return .{ .barcode = code, .slot = "A01", .queued_at = at };
}

test "isValid accepts a barcode with a correct check digit" {
    try testing.expect(barcode.isValid("LS000123408"));
}

test "isValid accepts a barcode whose last payload digit is non-zero" {
    try testing.expect(barcode.isValid("LS000123453"));
}

test "isValid rejects a wrong check digit and bad shapes" {
    try testing.expect(!barcode.isValid("LS000123409"));
    try testing.expect(!barcode.isValid("XX000123408"));
    try testing.expect(!barcode.isValid("LS00012340"));
}

test "next returns samples oldest first" {
    var q = queue.SampleQueue.init(testing.allocator, 10);
    defer q.deinit();
    try q.enqueue(mkSample("LS000123408", 1));
    try q.enqueue(mkSample("LS000456704", 2));
    try testing.expectEqualStrings("LS000123408", q.next().?.barcode);
    try testing.expectEqualStrings("LS000456704", q.next().?.barcode);
    try testing.expect(q.next() == null);
}

test "enqueue ignores a barcode that is already queued" {
    var q = queue.SampleQueue.init(testing.allocator, 10);
    defer q.deinit();
    try q.enqueue(mkSample("LS000123408", 1));
    try q.enqueue(mkSample("LS000123408", 2));
    try testing.expectEqual(@as(usize, 1), q.len());
}

test "countStale flags samples older than the threshold" {
    var q = queue.SampleQueue.init(testing.allocator, 10);
    defer q.deinit();
    try q.enqueue(mkSample("LS000123408", 0));
    try q.enqueue(mkSample("LS000456704", 5000));
    try testing.expectEqual(@as(usize, 1), q.countStale(6000, 30));
}

test "load splits the site list on commas" {
    var env = std.process.EnvMap.init(testing.allocator);
    defer env.deinit();
    try env.put("LABQ_ALLOWED_SITES", "main, north");
    const cfg = try config.load(testing.allocator, &env);
    defer testing.allocator.free(cfg.allowed_sites);
    try testing.expectEqual(@as(usize, 2), cfg.allowed_sites.len);
    try testing.expectEqual(@as(u32, 90), cfg.stale_after_min);
}

test "pendingOrders keeps only allowed sites" {
    const Fake = struct {
        fn fetch(_: ?[]const u8) lims.Page {
            return .{
                .orders = &.{
                    .{ .barcode = "LS000123408", .site = "main", .assay = "cbc" },
                    .{ .barcode = "LS000456704", .site = "remote", .assay = "cbc" },
                },
                .next_cursor = null,
            };
        }
    };
    const cfg = config.Config{ .allowed_sites = &.{"main"}, .export_dir = "/tmp", .stale_after_min = 90, .api_token = "test" };
    var out = std.ArrayList(lims.Order).init(testing.allocator);
    defer out.deinit();
    try lims.pendingOrders(cfg, .{ .fetchFn = &Fake.fetch }, &out);
    try testing.expectEqual(@as(usize, 1), out.items.len);
}

test "sanitizeName replaces punctuation" {
    var buf: [16]u8 = undefined;
    try testing.expectEqualStrings("ana_b-c", manifest.sanitizeName(&buf, "ana;b-c"));
}

const stub_tray = [_][]const u8{ "LS000123408", "LS000456704", "LS000987602" };

const StubScanner = struct {
    pub fn scan(_: StubScanner, _: std.mem.Allocator) ![]const []const u8 {
        return &stub_tray;
    }
};

test "ingestTray queues every tube on the tray" {
    var q = queue.SampleQueue.init(testing.allocator, 96);
    defer q.deinit();
    const n = try scanner.ingestTray(testing.allocator, StubScanner{}, &q, 100);
    try testing.expectEqual(@as(usize, 3), n);
    try testing.expectEqual(@as(usize, 3), q.len());
}
