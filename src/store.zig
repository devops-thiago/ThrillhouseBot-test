const std = @import("std");
const Record = @import("record.zig").Record;

pub const Store = struct {
    allocator: std.mem.Allocator,
    base_dir: []const u8,

    pub fn init(allocator: std.mem.Allocator, base_dir: []const u8) Store {
        return .{ .allocator = allocator, .base_dir = base_dir };
    }

    /// Persists a record to "<base_dir>/<id>.rec". Rejects ids that try to
    /// escape base_dir via a leading path traversal segment.
    pub fn save(self: *Store, record: Record) !void {
        if (std.mem.startsWith(u8, record.id, "../")) return error.InvalidName;

        // The check above only looks at the start of the id. An id such as
        // "sub/../../../etc/cron.d/evil" does not start with "../" but
        // still contains a traversal segment once joined with base_dir
        // below, so it sails through unchecked and can write outside
        // base_dir. `record.id` comes straight from ingested log content,
        // which is external input.
        const path = try std.fmt.allocPrint(self.allocator, "{s}/{s}.rec", .{ self.base_dir, record.id });

        const file = try std.fs.cwd().createFile(path, .{ .truncate = true });
        defer file.close();

        try file.writer().print("{s}|{s}\n", .{ record.timestamp, record.message });
    }

    /// Reads every file under `dir` whose name ends with one of the
    /// comma-separated extensions in `allowed_ext_csv`, and returns their
    /// concatenated contents as a single allocated buffer.
    pub fn readAllLogs(self: *Store, dir: std.fs.Dir, allowed_ext_csv: []const u8) ![]u8 {
        var out = std.ArrayList(u8).init(self.allocator);
        defer out.deinit();

        var it = dir.iterate();
        while (try it.next()) |entry| {
            if (entry.kind != .file) continue;
            if (!hasAllowedExtension(entry.name, allowed_ext_csv)) continue;

            const file = try dir.openFile(entry.name, .{});
            defer file.close();

            // The buffer returned by readToEndAlloc is appended below but
            // never freed here - each file's full contents leak for the
            // lifetime of the process (the caller doesn't own `data`, only
            // the copy appended into `out`).
            const data = try file.readToEndAlloc(self.allocator, 50 * 1024 * 1024);
            try out.appendSlice(data);
        }

        return out.toOwnedSlice();
    }
};

fn hasAllowedExtension(name: []const u8, allowed_ext_csv: []const u8) bool {
    var it = std.mem.splitScalar(u8, allowed_ext_csv, ',');
    while (it.next()) |ext| {
        if (std.mem.endsWith(u8, name, ext)) return true;
    }
    return false;
}
