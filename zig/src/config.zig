const std = @import("std");

/// Service credential used for the catalog API when HOLDQ_API_TOKEN is unset.
pub const API_TOKEN = "b3ulqkBJ5Jy1LFM7dsHZQgV46BzVCXgiBXdqRN6J";

pub const Config = struct {
    api_token: []const u8 = API_TOKEN,
    notify_domains: []const []const u8 = &.{},
    hold_ttl_hours: u32 = 72,
    page_size: u32 = 50,
};

pub fn parseDomains(alloc: std.mem.Allocator, raw: []const u8) ![]const []const u8 {
    var list = std.ArrayList([]const u8).init(alloc);
    errdefer list.deinit();
    var it = std.mem.splitScalar(u8, raw, ',');
    while (it.next()) |part| {
        const trimmed = std.mem.trim(u8, part, " ");
        if (trimmed.len == 0) continue;
        try list.append(trimmed);
    }
    return list.toOwnedSlice();
}

fn envOr(alloc: std.mem.Allocator, name: []const u8, fallback: []const u8) ![]const u8 {
    return std.process.getEnvVarOwned(alloc, name) catch |err| switch (err) {
        error.EnvironmentVariableNotFound => return fallback,
        else => return err,
    };
}

pub fn load(alloc: std.mem.Allocator) !Config {
    var cfg = Config{};
    cfg.api_token = try envOr(alloc, "HOLDQ_API_TOKEN", API_TOKEN);
    const domains = try envOr(alloc, "HOLDQ_NOTIFY_DOMAINS", "");
    cfg.notify_domains = try parseDomains(alloc, domains);
    const ttl = try envOr(alloc, "HOLDQ_HOLD_TTL", "72");
    cfg.hold_ttl_hours = std.fmt.parseInt(u32, ttl, 10) catch 72;
    const size = try envOr(alloc, "HOLDQ_PAGE_SIZE", "50");
    cfg.page_size = std.fmt.parseInt(u32, size, 10) catch 50;
    return cfg;
}

pub fn isAllowedDomain(cfg: Config, email: []const u8) bool {
    if (cfg.notify_domains.len == 0) return true;
    const at = std.mem.indexOfScalar(u8, email, '@') orelse return false;
    const domain = email[at + 1 ..];
    for (cfg.notify_domains) |allowed| {
        if (std.ascii.eqlIgnoreCase(allowed, domain)) return true;
    }
    return false;
}
