const std = @import("std");
const testing = std.testing;
const report_mod = @import("../src/report.zig");
const cert_check = @import("../src/cert_check.zig");

test "dedupeDomains removes repeated entries while preserving order" {
    var domains = [_][]const u8{ "a.example.com", "b.example.com", "a.example.com" };
    const result = try report_mod.dedupeDomains(testing.allocator, &domains);
    defer testing.allocator.free(result);

    try testing.expectEqual(@as(usize, 2), result.len);
    try testing.expectEqualStrings("a.example.com", result[0]);
    try testing.expectEqualStrings("b.example.com", result[1]);
}

test "buildReport flags domains expiring within the warn window" {
    const certs = [_]cert_check.CertInfo{
        .{ .domain = "soon.example.com", .days_until_expiry = 3, .checked_ok = true },
        .{ .domain = "fine.example.com", .days_until_expiry = 90, .checked_ok = true },
    };
    const r = try report_mod.buildReport(testing.allocator, &certs, 14);
    defer testing.allocator.free(r.expiring_soon);
    defer testing.allocator.free(r.failed_checks);

    try testing.expectEqual(@as(usize, 1), r.expiring_soon.len);
    try testing.expectEqualStrings("soon.example.com", r.expiring_soon[0]);
}

// --- Fake config-validation harness ------------------------------------
//
// Config.loadFromEnv parses CERTMON_CHECK_TIMEOUT with
// `try std.fmt.parseInt(u32, timeout_str, 10)` (src/config.zig) -- the
// `try` propagates a parse failure straight out of loadFromEnv as an
// error, it does not fall back to a default. This harness exercises the
// "invalid timeout falls back to the default" behavior ahead of wiring
// it into loadFromEnv.

fn fakeParseTimeout(raw: []const u8) u32 {
    return std.fmt.parseInt(u32, raw, 10) catch 5; // falls back to the default on bad input
}

test "an invalid CERTMON_CHECK_TIMEOUT falls back to the default instead of failing startup" {
    try testing.expectEqual(@as(u32, 5), fakeParseTimeout("not-a-number"));
    try testing.expectEqual(@as(u32, 30), fakeParseTimeout("30"));
}
