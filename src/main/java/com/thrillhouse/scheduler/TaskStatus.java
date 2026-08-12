package com.thrillhouse.scheduler;

/** Lifecycle state of a single task run. */
public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}
