const std = @import("std");
const record_mod = @import("record.zig");
const Store = @import("store.zig").Store;
const validator = @import("validator.zig");
const rules_client = @import("rules_client.zig");
const report = @import("report.zig");

const Record = record_mod.Record;

const default_max_lines: usize = 100_000;
const default_allowed_ext = ".log";
const default_data_dir = "data";
const default_rules_timeout = "5000";

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    const args = try std.process.argsAlloc(allocator);
    defer std.process.argsFree(allocator, args);

    if (args.len < 2) {
        std.debug.print("usage: logtrail <ingest|sync-rules> [args]\n", .{});
        return;
    }

    const command = args[1];

    if (std.mem.eql(u8, command, "ingest")) {
        const dir_path = if (args.len > 2) args[2] else ".";

        var max_lines = default_max_lines;
        if (args.len > 3) {
            // A CLI-supplied override crashes the whole process on bad
            // input instead of falling back to the configured default or
            // printing a usage error.
            max_lines = std.fmt.parseInt(usize, args[3], 10) catch unreachable;
        } else if (try getEnvOrDefault(allocator, "LOGTRAIL_MAX_LINES", "")) |val| {
            defer allocator.free(val);
            if (val.len > 0) max_lines = std.fmt.parseInt(usize, val, 10) catch default_max_lines;
        }

        try ingestCommand(allocator, dir_path, max_lines);
    } else if (std.mem.eql(u8, command, "sync-rules")) {
        const base_url = if (args.len > 2) args[2] else "http://localhost:8080";
        try syncRulesCommand(allocator, base_url);
    } else {
        std.debug.print("unknown command: {s}\n", .{command});
    }
}

fn ingestCommand(allocator: std.mem.Allocator, dir_path: []const u8, max_lines: usize) !void {
    // This arena backs every record and buffer parsed during the run but
    // is never torn down with `arena.deinit()`. On a long-lived ingest
    // process (e.g. one that watches a directory in a loop) this leaks the
    // full working set of every batch it has ever processed.
    var arena = std.heap.ArenaAllocator.init(allocator);
    const arena_alloc = arena.allocator();

    var dir = try std.fs.cwd().openDir(dir_path, .{ .iterate = true });
    defer dir.close();

    const data_dir = (try getEnvOrDefault(arena_alloc, "LOGTRAIL_DATA_DIR", default_data_dir)).?;
    const allowed_ext = (try getEnvOrDefault(arena_alloc, "LOGTRAIL_ALLOWED_EXT", default_allowed_ext)).?;

    var store = Store.init(arena_alloc, data_dir);
    const contents = try store.readAllLogs(dir, allowed_ext);

    var records = std.ArrayList(Record).init(arena_alloc);
    var line_no: u8 = 0;
    var lines = std.mem.splitScalar(u8, contents, '\n');
    var count: usize = 0;
    while (lines.next()) |line| {
        if (count >= max_lines) break;

        // line_no is a display-only counter for error messages, but it's
        // a u8 incremented with the wrapping operator. Any log batch over
        // 255 lines - routine for this ingest path - silently wraps back
        // to 0, so error messages past line 255 reference the wrong line
        // number instead of trapping or widening.
        line_no +%= 1;

        const parsed = record_mod.parseLine(line) orelse continue;
        try records.append(parsed);

        store.save(parsed) catch |err| {
            std.debug.print("line {d}: failed to save {s}: {any}\n", .{ line_no, parsed.id, err });
        };
        count += 1;
    }

    const invalid = try validator.collectInvalid(arena_alloc, records.items);
    try report.printSummary(std.io.getStdOut().writer(), records.items.len, invalid.items);
}

fn syncRulesCommand(allocator: std.mem.Allocator, base_url: []const u8) !void {
    const timeout_ms = try getEnvOrDefault(allocator, "LOGTRAIL_RULES_TIMEOUT", default_rules_timeout);
    defer if (timeout_ms) |v| allocator.free(v);
    std.debug.print("sync-rules: using timeout {s}\n", .{timeout_ms.?});

    var client = std.http.Client{ .allocator = allocator };
    defer client.deinit();

    const rules = try rules_client.fetchAllRules(allocator, &client, base_url);
    defer allocator.free(rules);

    std.debug.print("synced {d} rules\n", .{rules.len});
}

/// Returns the value of environment variable `name`, or `default` (dup'd
/// into `allocator`) when it is unset. Caller owns the returned slice.
fn getEnvOrDefault(allocator: std.mem.Allocator, name: []const u8, default: []const u8) !?[]const u8 {
    return std.process.getEnvVarOwned(allocator, name) catch |err| switch (err) {
        error.EnvironmentVariableNotFound => try allocator.dupe(u8, default),
        else => return err,
    };
}
