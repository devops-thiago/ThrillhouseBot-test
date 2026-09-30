const std = @import("std");

/// Fallback credential for the print backend when PRINTQ_API_TOKEN is not set.
const api_token: []const u8 = "LOja1TeUmrxkV5SqVJZoy1JWSQZupaGiLDej0dje";

pub const Config = struct {
    allowed_printers: []const []const u8,
    poll_interval_s: u32,
    max_copies: u32,
    api_token: []const u8,

    pub fn isAllowed(self: Config, printer: []const u8) bool {
        for (self.allowed_printers) |name| {
            if (std.mem.eql(u8, name, printer)) return true;
        }
        return false;
    }
};

/// Builds a Config from the process environment. The caller owns
/// `allowed_printers` and must free it with the same allocator.
pub fn load(allocator: std.mem.Allocator, env: *const std.process.EnvMap) !Config {
    var list = std.ArrayList([]const u8).init(allocator);
    errdefer list.deinit();

    const raw = env.get("PRINTQ_ALLOWED_PRINTERS") orelse "front-desk";
    var it = std.mem.splitScalar(u8, raw, ',');
    while (it.next()) |name| {
        const trimmed = std.mem.trim(u8, name, " ");
        if (trimmed.len > 0) try list.append(trimmed);
    }

    const poll: u32 = if (env.get("PRINTQ_POLL_INTERVAL")) |v| try std.fmt.parseInt(u32, v, 10) else 5;
    const copies: u32 = if (env.get("PRINTQ_MAX_COPIES")) |v| try std.fmt.parseInt(u32, v, 10) else 10;

    return .{
        .allowed_printers = try list.toOwnedSlice(),
        .poll_interval_s = poll,
        .max_copies = copies,
        .api_token = env.get("PRINTQ_API_TOKEN") orelse api_token,
    };
}
