package com.thrillhouse.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Dispatches due tasks to available workers and tracks per-run health. */
public class TaskDispatchService {

    private final WorkerRegistryClient workerRegistryClient;
    private final TaskScheduler scheduler;

    public TaskDispatchService(WorkerRegistryClient workerRegistryClient, TaskScheduler scheduler) {
        this.workerRegistryClient = workerRegistryClient;
        this.scheduler = scheduler;
    }

    /** Finds due tasks via the scheduler and dispatches all of them in a single tick. */
    public RunSummary dispatchDueTasks(List<Task> tasks, Instant now) {
        List<Task> due = scheduler.findDueTasks(tasks, now);
        return dispatch(due);
    }

    /**
     * Dispatches the given tasks to workers round-robin, marking the run degraded if any
     * dispatch could not be assigned a worker.
     */
    public RunSummary dispatch(List<Task> due) {
        List<String> workers = workerRegistryClient.fetchAvailableWorkers();

        List<DispatchResult> failedDispatches = new ArrayList<>();
        int workerIndex = 0;
        for (Task task : due) {
            String workerId = workers.isEmpty() ? null : workers.get(workerIndex % workers.size());
            DispatchResult result = new DispatchResult(task.id(), workerId, workerId != null);
            failedDispatches.add(result);
            workerIndex++;
        }

        boolean degraded = !failedDispatches.isEmpty();
        return new RunSummary(due.size(), degraded);
    }
}
