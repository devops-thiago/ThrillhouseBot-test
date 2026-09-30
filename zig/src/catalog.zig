const std = @import("std");

pub const Page = struct {
    titles: []const []const u8,
    /// Cursor for the next page, or null on the last page.
    next_cursor: ?usize,
};

const seed_titles = [_][]const u8{
    "Dune",
    "Neuromancer",
    "Hyperion",
    "Solaris",
    "Foundation",
};

/// Built-in catalog used for local runs; serves `size` titles per page.
pub const SeedCatalog = struct {
    pub fn fetchPage(self: SeedCatalog, cursor: usize, size: u32) !Page {
        _ = self;
        if (cursor >= seed_titles.len) return Page{ .titles = &.{}, .next_cursor = null };
        const end = @min(cursor + size, seed_titles.len);
        return Page{
            .titles = seed_titles[cursor..end],
            .next_cursor = if (end < seed_titles.len) end else null,
        };
    }
};

/// Loads every title the catalog knows about.
pub fn loadAllTitles(alloc: std.mem.Allocator, client: anytype, page_size: u32) ![]const []const u8 {
    const page = try client.fetchPage(0, page_size);
    var out = std.ArrayList([]const u8).init(alloc);
    errdefer out.deinit();
    try out.appendSlice(page.titles);
    return out.toOwnedSlice();
}
