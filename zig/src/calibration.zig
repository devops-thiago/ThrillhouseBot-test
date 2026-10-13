/// Converts raw hub readings into degrees Celsius.
///
/// The sensor hub reports temperatures as signed integers in TENTHS of a
/// degree (215 means 21.5 C). `offset_c` is the per-probe correction, in
/// degrees Celsius, measured when the probe was installed.
pub const Calibration = struct {
    offset_c: f64 = 0.0,

    pub fn toCelsius(self: Calibration, raw_tenths: i32) f64 {
        return @as(f64, @floatFromInt(raw_tenths)) / 10.0 + self.offset_c;
    }
};
