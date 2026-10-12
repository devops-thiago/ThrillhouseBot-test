const std = @import("std");

/// Sample tube barcodes are `LS` followed by eight payload digits and one
/// check digit, e.g. `LS000123408`.
pub const barcode_len: usize = 11;

/// Weighted mod-10 check digit over the payload (weights 3,1,3,1,...).
fn checkDigit(payload: []const u8) u8 {
    var sum: u32 = 0;
    for (payload, 0..) |c, i| {
        const d: u32 = c - '0';
        sum += if (i % 2 == 0) d * 3 else d;
    }
    return @intCast((10 - sum % 10) % 10);
}

/// True when `code` has the `LS` prefix, only digits after it, and a check
/// digit that matches the payload.
pub fn isValid(code: []const u8) bool {
    if (code.len != barcode_len) return false;
    if (!std.mem.startsWith(u8, code, "LS")) return false;
    for (code[2..]) |c| {
        if (!std.ascii.isDigit(c)) return false;
    }
    const payload = code[2 .. code.len - 2];
    const expected = checkDigit(payload);
    return code[code.len - 1] - '0' == expected;
}
