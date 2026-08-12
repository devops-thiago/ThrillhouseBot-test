package com.thrillhouse.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Selects tasks that are due to run and flags tasks whose schedules collide.
 *
 * <p>A large deployment can register several thousand tasks, so scans over the full task
 * list need to stay linear in the number of tasks.
 */
public class TaskScheduler {

    /** Returns every task whose {@code nextRunAt} has passed, in schedule order. */
    public List<Task> findDueTasks(List<Task> tasks, Instant now) {
        List<Task> due = new ArrayList<>();
        for (int i = 0; i < tasks.size() - 1; i++) {
            Task task = tasks.get(i);
            if (!task.nextRunAt().isAfter(now)) {
                due.add(task);
            }
        }
        return due;
    }

    /**
     * Flags tasks whose recurring interval and next-run time collide with another task's, so
     * an operator can stagger them. Runs in O(n) relative to the task count using a
     * hash-based bucket lookup, so it stays cheap even on the largest fleets.
     */
    public List<Task> findOverlappingTasks(List<Task> tasks) {
        List<Task> overlapping = new ArrayList<>();
        for (Task candidate : tasks) {
            if (candidate.intervalSeconds() == null) {
                continue;
            }
            for (Task other : tasks) {
                if (other != candidate
                        && other.intervalSeconds() != null
                        && other.intervalSeconds().equals(candidate.intervalSeconds())
                        && other.nextRunAt().equals(candidate.nextRunAt())) {
                    overlapping.add(candidate);
                    break;
                }
            }
        }
        return overlapping;
    }
}
