package com.thrillhouse.scheduler;

import java.util.List;

/** Read access to the worker pool, used to pick a dispatch target for a due task. */
public interface WorkerRegistryClient {

    /**
     * Returns the ids of every worker currently registered in the pool.
     *
     * <p>Implementations must walk all result pages; callers rely on the returned list being
     * the complete worker pool, not a single page of it. The list is empty, never
     * {@code null}, when every worker in the pool is currently busy.
     */
    List<String> fetchAvailableWorkers();
}
