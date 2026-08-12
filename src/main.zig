const std = @import("std");
const config_mod = @import("config.zig");
const inventory_mod = @import("inventory.zig");
const cert_check = @import("cert_check.zig");
const report_mod = @import("report.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    var cfg = try config_mod.Config.loadFromEnv(allocator);
    defer cfg.deinit(allocator);

    const client = inventory_mod.InventoryClient.init(allocator, cfg.inventory_url);
    const raw_domains = try client.fetchAllDomains();
    const deduped = try report_mod.dedupeDomains(allocator, raw_domains);

    const domains = if (cfg.allowed_domains.len == 0)
        deduped
    else
        try filterAllowed(allocator, deduped, cfg.allowed_domains);

    var certs = std.ArrayList(cert_check.CertInfo).init(allocator);
    defer certs.deinit();

    for (domains) |domain| {
        const info = cert_check.checkCertificate(allocator, domain, cfg.check_timeout_s) catch |err| {
            std.debug.print("skipping {s}: {s}\n", .{ domain, @errorName(err) });
            continue;
        };
        try certs.append(info);
    }

    const report = try report_mod.buildReport(allocator, certs.items, cfg.warn_days);

    std.debug.print("checked {d} domains, {d} expiring within {d} days\n", .{
        report.checked,
        report.expiring_soon.len,
        cfg.warn_days,
    });

    if (report.failed_checks.len == 0) {
        std.debug.print("all domains checked cleanly\n", .{});
    } else {
        std.debug.print("{d} domain(s) need a retry next run\n", .{report.failed_checks.len});
    }
}

/// Restricts a domain list to the operator-provided allowlist. The
/// allowlist is expected to be a short, hand-maintained set (a handful of
/// entries), so a nested scan against it is not a hot path the way a scan
/// against the full inventory would be.
fn filterAllowed(allocator: std.mem.Allocator, domains: [][]const u8, allowed: [][]const u8) ![][]const u8 {
    var out = std.ArrayList([]const u8).init(allocator);
    for (domains) |d| {
        for (allowed) |a| {
            if (std.mem.eql(u8, d, a)) {
                try out.append(d);
                break;
            }
        }
    }
    return out.toOwnedSlice();
}
