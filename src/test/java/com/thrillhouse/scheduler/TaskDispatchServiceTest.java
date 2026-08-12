package com.thrillhouse.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskDispatchServiceTest {

    @Test
    void dispatch_countsEveryDueTask() {
        WorkerRegistryClient mockRegistry = mock(WorkerRegistryClient.class);
        // Stub the pool as always having a worker on hand, even though the real registry
        // contract allows fetchAvailableWorkers() to come back empty when every worker in
        // the pool is currently busy — dispatch is documented to defer cleanly in that case,
        // a path this stub can never exercise.
        when(mockRegistry.fetchAvailableWorkers()).thenReturn(List.of("worker-1"));

        TaskDispatchService service = new TaskDispatchService(mockRegistry, new TaskScheduler());
        Task task = new Task("t-1", "cleanup", 60, Instant.now().minusSeconds(5), "{}");

        RunSummary summary = service.dispatch(List.of(task));

        assertEquals(1, summary.dispatchedCount());
        verify(mockRegistry).fetchAvailableWorkers();
    }

    @Test
    void findOverlappingTasks_flagsSharedSchedule() {
        Instant now = Instant.now();
        Task first = new Task("t-1", "backup", 300, now, "{}");
        Task second = new Task("t-2", "backup-mirror", 300, now, "{}");
        Task third = new Task("t-3", "one-off", null, now.plusSeconds(60), "{}");

        List<Task> overlapping = new TaskScheduler().findOverlappingTasks(List.of(first, second, third));

        assertEquals(2, overlapping.size());
    }
}
