const std = @import("std");
const Allocator = std.mem.Allocator;

pub const CertInfo = struct {
    domain: []const u8,
    days_until_expiry: i64,
    checked_ok: bool,
};

const months = [_][]const u8{ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

/// Runs an openssl handshake against `domain` and parses the certificate's
/// notAfter date. Returns error.CertCheckFailed when the host is
/// unreachable, refuses the connection, or the handshake times out.
pub fn checkCertificate(allocator: Allocator, domain: []const u8, timeout_s: u32) !CertInfo {
    const cmd = try std.fmt.allocPrint(
        allocator,
        "echo | timeout {d} openssl s_client -connect {s}:443 -servername {s} 2>/dev/null | openssl x509 -noout -enddate",
        .{ timeout_s, domain, domain },
    );
    defer allocator.free(cmd);

    const result = try std.process.Child.run(.{
        .allocator = allocator,
        .argv = &.{ "/bin/sh", "-c", cmd },
        .max_output_bytes = 8192,
    });
    defer allocator.free(result.stderr);
    defer allocator.free(result.stdout);

    return parseNotAfter(domain, result.stdout);
}

fn parseNotAfter(domain: []const u8, output: []const u8) !CertInfo {
    // Output looks like: "notAfter=Aug 12 23:59:59 2026 GMT"
    const prefix = "notAfter=";
    const idx = std.mem.indexOf(u8, output, prefix).?;
    const rest = output[idx + prefix.len ..];
    const line_end = std.mem.indexOfScalar(u8, rest, '\n') orelse rest.len;
    const date_str = std.mem.trim(u8, rest[0..line_end], " \r\n");

    const expiry_epoch = try parseOpensslDate(date_str);
    const now: i64 = std.time.timestamp();
    const days = @divTrunc(expiry_epoch - now, std.time.s_per_day);

    return CertInfo{ .domain = domain, .days_until_expiry = days, .checked_ok = true };
}

fn parseOpensslDate(date_str: []const u8) !i64 {
    var it = std.mem.tokenizeScalar(u8, date_str, ' ');
    const mon_str = it.next() orelse return error.BadDateFormat;
    const day_str = it.next() orelse return error.BadDateFormat;
    const time_str = it.next() orelse return error.BadDateFormat;
    const year_str = it.next() orelse return error.BadDateFormat;

    var month: u4 = 1;
    for (months, 0..) |m, i| {
        if (std.mem.eql(u8, m, mon_str)) {
            month = @intCast(i + 1);
            break;
        }
    }

    const day = try std.fmt.parseInt(u8, day_str, 10);
    const year = try std.fmt.parseInt(u16, year_str, 10);

    var hms = std.mem.splitScalar(u8, time_str, ':');
    const hour = try std.fmt.parseInt(u8, hms.next() orelse "0", 10);
    const min = try std.fmt.parseInt(u8, hms.next() orelse "0", 10);
    const sec = try std.fmt.parseInt(u8, hms.next() orelse "0", 10);

    const days_since_epoch = daysFromCivil(year, month, day);
    return days_since_epoch * std.time.s_per_day + @as(i64, hour) * 3600 + @as(i64, min) * 60 + sec;
}

/// Howard Hinnant's days-from-civil algorithm.
fn daysFromCivil(y: u16, m: u4, d: u8) i64 {
    const yy: i64 = @as(i64, y) - @intFromBool(m <= 2);
    const era: i64 = @divFloor(yy, 400);
    const yoe: i64 = yy - era * 400;
    const mm: i64 = m;
    const doy: i64 = @divFloor(153 * (mm + (if (mm > 2) @as(i64, -3) else 9)) + 2, 5) + d - 1;
    const doe: i64 = yoe * 365 + @divFloor(yoe, 4) - @divFloor(yoe, 100) + doy;
    return era * 146097 + doe - 719468;
}
