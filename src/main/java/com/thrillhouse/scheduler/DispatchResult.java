package com.thrillhouse.scheduler;

/** Outcome of dispatching one task to a worker. */
public record DispatchResult(String taskId, String workerId, boolean succeeded) {
}
