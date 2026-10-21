const std = @import("std");

/// One weight sample from a hive scale.
pub const Reading = struct {
    hive_id: []const u8,
    ts: i64,
    grams: f64,
};

pub const ParseError = error{ MissingField, BadTimestamp, BadWeight };

/// Parses a line of the form "hive_id,unix_ts,grams".
pub fn parseLine(line: []const u8) ParseError!Reading {
    var it = std.mem.splitScalar(u8, line, ',');
    const hive = it.next() orelse return error.MissingField;
    const ts_text = it.next() orelse return error.MissingField;
    const grams_text = it.next() orelse return error.MissingField;
    const ts = std.fmt.parseInt(i64, std.mem.trim(u8, ts_text, " "), 10) catch return error.BadTimestamp;
    const grams = std.fmt.parseFloat(f64, std.mem.trim(u8, grams_text, " ")) catch return error.BadWeight;
    return .{ .hive_id = std.mem.trim(u8, hive, " "), .ts = ts, .grams = grams };
}

/// Hive ids are short labels such as "hive-17"; anything longer than 64 bytes is rejected.
pub fn looksLikeHiveId(id: []const u8) bool {
    return id.len > 0 and id.len <= 64;
}

/// A scale reading is plausible when it is a non-negative weight under 200 kg.
pub fn isPlausible(r: Reading) bool {
    return r.grams >= 0 and r.grams < 200_000;
}
