const std = @import("std");
const testing = std.testing;
const compactor = @import("compactor.zig");
const config = @import("config.zig");
const Calibration = @import("calibration.zig").Calibration;
const Reading = compactor.Reading;

const FakeCalibration = struct {
    pub fn toCelsius(_: FakeCalibration, raw_tenths: i32) f64 {
        return @floatFromInt(raw_tenths);
    }
};

test "parseSensorIds splits and trims entries" {
    const ids = try config.parseSensorIds(testing.allocator, "gh-01, gh-02,,gh-03 ");
    defer testing.allocator.free(ids);
    try testing.expectEqual(@as(usize, 3), ids.len);
    try testing.expectEqualStrings("gh-02", ids[1]);
    try testing.expectEqualStrings("gh-03", ids[2]);
}

test "dedupe drops repeated timestamps" {
    const input = [_]Reading{
        .{ .sensor_id = "gh-01", .ts = 100, .raw_tenths = 200 },
        .{ .sensor_id = "gh-01", .ts = 100, .raw_tenths = 201 },
        .{ .sensor_id = "gh-01", .ts = 160, .raw_tenths = 205 },
    };
    const out = try compactor.dedupe(testing.allocator, &input);
    defer testing.allocator.free(out);
    try testing.expectEqual(@as(usize, 2), out.len);
    try testing.expectEqual(@as(i32, 200), out[0].raw_tenths);
}

test "sortByTime orders readings chronologically" {
    var input = [_]Reading{
        .{ .sensor_id = "gh-01", .ts = 300, .raw_tenths = 210 },
        .{ .sensor_id = "gh-01", .ts = 100, .raw_tenths = 200 },
        .{ .sensor_id = "gh-01", .ts = 200, .raw_tenths = 205 },
    };
    compactor.sortByTime(&input);
    try testing.expectEqual(@as(i64, 100), input[0].ts);
    try testing.expectEqual(@as(i64, 300), input[2].ts);
}

test "compact counts every reading in the window" {
    const input = [_]Reading{
        .{ .sensor_id = "gh-01", .ts = 0, .raw_tenths = 200 },
        .{ .sensor_id = "gh-01", .ts = 60, .raw_tenths = 210 },
        .{ .sensor_id = "gh-01", .ts = 120, .raw_tenths = 220 },
    };
    const buckets = try compactor.compact(testing.allocator, &input, 300, Calibration{});
    defer testing.allocator.free(buckets);
    try testing.expectEqual(@as(usize, 1), buckets.len);
    try testing.expectEqual(@as(u32, 3), buckets[0].count);
}

test "compact on an empty batch returns no buckets" {
    const buckets = try compactor.compact(testing.allocator, &[_]Reading{}, 300, Calibration{});
    defer testing.allocator.free(buckets);
    try testing.expectEqual(@as(usize, 0), buckets.len);
}

test "compact reports window minimum in celsius" {
    const input = [_]Reading{
        .{ .sensor_id = "gh-02", .ts = 0, .raw_tenths = 210 },
        .{ .sensor_id = "gh-02", .ts = 60, .raw_tenths = 220 },
        .{ .sensor_id = "gh-02", .ts = 120, .raw_tenths = 230 },
    };
    const buckets = try compactor.compact(testing.allocator, &input, 300, FakeCalibration{});
    defer testing.allocator.free(buckets);
    try testing.expectEqual(@as(usize, 1), buckets.len);
    try testing.expectEqual(@as(f64, 210.0), buckets[0].min_c);
}
