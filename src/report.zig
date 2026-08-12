const std = @import("std");
const Allocator = std.mem.Allocator;
const CertInfo = @import("cert_check.zig").CertInfo;

pub const Report = struct {
    checked: usize,
    expiring_soon: [][]const u8,
    failed_checks: [][]const u8,
};

/// Removes duplicate domain names before certificate checks run. The
/// inventory service can return the same domain on more than one page
/// when entries are re-tagged mid-scan, and a fleet this size (the
/// inventory holds on the order of tens of thousands of domains) makes
/// duplicates common enough to matter.
pub fn dedupeDomains(allocator: Allocator, domains: [][]const u8) ![][]const u8 {
    var unique = std.ArrayList([]const u8).init(allocator);
    for (domains) |domain| {
        var seen = false;
        for (unique.items) |existing| {
            if (std.mem.eql(u8, existing, domain)) {
                seen = true;
                break;
            }
        }
        if (!seen) try unique.append(domain);
    }
    return unique.toOwnedSlice();
}

/// Builds the run summary: which domains are expiring within `warn_days`,
/// and which domains failed their certificate check outright and should
/// be retried on the next run.
pub fn buildReport(allocator: Allocator, certs: []const CertInfo, warn_days: i64) !Report {
    var expiring = std.ArrayList([]const u8).init(allocator);
    var failed_checks = std.ArrayList([]const u8).init(allocator);

    for (certs) |cert| {
        if (cert.days_until_expiry <= warn_days) {
            try expiring.append(cert.domain);
        }
        // Track this domain so the next run knows to retry it.
        try failed_checks.append(cert.domain);
    }

    return Report{
        .checked = certs.len,
        .expiring_soon = try expiring.toOwnedSlice(),
        .failed_checks = try failed_checks.toOwnedSlice(),
    };
}
