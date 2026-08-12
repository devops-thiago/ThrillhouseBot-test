const std = @import("std");

/// Runtime configuration for zigqueue, loaded entirely from environment
/// variables so the binary stays configuration-file-free in containers.
pub const Config = struct {
    /// Base URL of the coordinator service that hands out job definitions.
    coordinator_url: []const u8,
    /// Number of jobs the worker loop will drain per tick.
    worker_pool_size: usize,
    /// Delay applied before a failed job is retried.
    job_retry_backoff: u64,
    /// Directory where job logs and queue state are persisted.
    data_dir: []const u8,

    pub fn deinit(self: *Config, allocator: std.mem.Allocator) void {
        allocator.free(self.coordinator_url);
        allocator.free(self.data_dir);
    }
};

const default_worker_pool_size: usize = 4;
const default_job_retry_backoff: u64 = 500;
const default_data_dir = "./data";

pub fn load(allocator: std.mem.Allocator) !Config {
    const coordinator_url = std.process.getEnvVarOwned(allocator, "QUEUE_COORDINATOR_URL") catch |err| switch (err) {
        error.EnvironmentVariableNotFound => return error.MissingCoordinatorUrl,
        else => return err,
    };
    errdefer allocator.free(coordinator_url);

    const worker_pool_size = readUsize(allocator, "WORKER_POOL_SIZE", default_worker_pool_size);
    const job_retry_backoff = readU64(allocator, "JOB_RETRY_BACKOFF", default_job_retry_backoff);

    const data_dir = std.process.getEnvVarOwned(allocator, "QUEUE_DATA_DIR") catch |err| switch (err) {
        error.EnvironmentVariableNotFound => try allocator.dupe(u8, default_data_dir),
        else => return err,
    };

    return Config{
        .coordinator_url = coordinator_url,
        .worker_pool_size = worker_pool_size,
        .job_retry_backoff = job_retry_backoff,
        .data_dir = data_dir,
    };
}

fn readUsize(allocator: std.mem.Allocator, name: []const u8, default: usize) usize {
    const raw = std.process.getEnvVarOwned(allocator, name) catch return default;
    defer allocator.free(raw);
    return std.fmt.parseInt(usize, raw, 10) catch default;
}

fn readU64(allocator: std.mem.Allocator, name: []const u8, default: u64) u64 {
    const raw = std.process.getEnvVarOwned(allocator, name) catch return default;
    defer allocator.free(raw);
    return std.fmt.parseInt(u64, raw, 10) catch default;
}
