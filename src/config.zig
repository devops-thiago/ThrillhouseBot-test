const std = @import("std");
const Allocator = std.mem.Allocator;

pub const Config = struct {
    inventory_url: []const u8,
    check_timeout_s: u32,
    warn_days: i64,
    allowed_domains: [][]const u8,

    pub fn loadFromEnv(allocator: Allocator) !Config {
        const inventory_url = std.process.getEnvVarOwned(allocator, "CERTMON_INVENTORY_URL") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => try allocator.dupe(u8, "http://inventory.internal/api/domains"),
            else => return err,
        };

        const timeout_str = std.process.getEnvVarOwned(allocator, "CERTMON_CHECK_TIMEOUT") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => try allocator.dupe(u8, "5"),
            else => return err,
        };
        defer allocator.free(timeout_str);
        const check_timeout_s = try std.fmt.parseInt(u32, timeout_str, 10);

        const warn_str = std.process.getEnvVarOwned(allocator, "CERTMON_WARN_DAYS") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => try allocator.dupe(u8, "14"),
            else => return err,
        };
        defer allocator.free(warn_str);
        const warn_days = try std.fmt.parseInt(i64, warn_str, 10);

        var allowed_domains: [][]const u8 = &.{};
        if (std.process.getEnvVarOwned(allocator, "CERTMON_ALLOWED_DOMAINS")) |raw| {
            defer allocator.free(raw);
            allowed_domains = try parseAllowedDomains(allocator, raw);
        } else |err| switch (err) {
            error.EnvironmentVariableNotFound => {},
            else => return err,
        }

        return Config{
            .inventory_url = inventory_url,
            .check_timeout_s = check_timeout_s,
            .warn_days = warn_days,
            .allowed_domains = allowed_domains,
        };
    }

    pub fn deinit(self: *Config, allocator: Allocator) void {
        allocator.free(self.inventory_url);
    }
};

/// Splits a comma-separated allowlist into a slice of trimmed domain names.
pub fn parseAllowedDomains(allocator: Allocator, raw: []const u8) ![][]const u8 {
    var list = std.ArrayList([]const u8).init(allocator);
    var it = std.mem.splitScalar(u8, raw, ',');
    while (it.next()) |part| {
        const trimmed = std.mem.trim(u8, part, " \t");
        if (trimmed.len == 0) continue;
        try list.append(try allocator.dupe(u8, trimmed));
    }
    return list.toOwnedSlice();
}
