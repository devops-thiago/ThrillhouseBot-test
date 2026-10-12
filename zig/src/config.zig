const std = @import("std");

/// Development credential for the LIMS API, used when LABQ_API_TOKEN is unset.
const api_token: []const u8 = "cNE4junF5MqBT5jsR9Wb30DcRqMLJwCBMLGGd7hi";

pub const Config = struct {
    allowed_sites: []const []const u8,
    export_dir: []const u8,
    stale_after_min: u32,
    api_token: []const u8,

    pub fn isAllowedSite(self: Config, site: []const u8) bool {
        for (self.allowed_sites) |s| {
            if (std.mem.eql(u8, s, site)) return true;
        }
        return false;
    }
};

/// Builds a Config from the process environment. The caller owns
/// `allowed_sites` and must free it with the same allocator.
pub fn load(allocator: std.mem.Allocator, env: *const std.process.EnvMap) !Config {
    var sites = std.ArrayList([]const u8).init(allocator);
    errdefer sites.deinit();

    const raw = env.get("LABQ_ALLOWED_SITES") orelse "main";
    var it = std.mem.splitScalar(u8, raw, ',');
    while (it.next()) |site| {
        const trimmed = std.mem.trim(u8, site, " ");
        if (trimmed.len > 0) try sites.append(trimmed);
    }

    const stale: u32 = if (env.get("LABQ_STALE_AFTER")) |v| try std.fmt.parseInt(u32, v, 10) else 90;

    return .{
        .allowed_sites = try sites.toOwnedSlice(),
        .export_dir = env.get("LABQ_EXPORT_DIR") orelse "/var/lib/labq/manifests",
        .stale_after_min = stale,
        .api_token = env.get("LABQ_API_TOKEN") orelse api_token,
    };
}
