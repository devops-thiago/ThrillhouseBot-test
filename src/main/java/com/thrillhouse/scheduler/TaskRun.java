package com.thrillhouse.scheduler;

import java.time.Instant;

/** A single execution record for a task, persisted for the operator run-history view. */
public record TaskRun(String taskId, Instant startedAt, TaskStatus status) {
}
