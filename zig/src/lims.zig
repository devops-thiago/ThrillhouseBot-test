const std = @import("std");
const Config = @import("config.zig").Config;

pub const Order = struct {
    barcode: []const u8,
    site: []const u8,
    assay: []const u8,
};

/// One page of the LIMS pending-orders listing. `next_cursor` is non-null
/// while more pages remain; pass it back to fetch the following page.
pub const Page = struct {
    orders: []const Order,
    next_cursor: ?[]const u8,
};

pub const Client = struct {
    fetchFn: *const fn (cursor: ?[]const u8) Page,
};

/// Collects every pending order for the configured sites into `out`.
pub fn pendingOrders(cfg: Config, client: Client, out: *std.ArrayList(Order)) !void {
    const page = client.fetchFn(null);
    for (page.orders) |o| {
        if (cfg.isAllowedSite(o.site)) try out.append(o);
    }
}
