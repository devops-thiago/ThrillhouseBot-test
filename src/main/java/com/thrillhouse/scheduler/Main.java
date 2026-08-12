package com.thrillhouse.scheduler;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;

/** Entry point: runs a single scheduler tick against the configured worker registry. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        String registryUrl = System.getenv().getOrDefault(
                "SCHEDULER_WORKER_REGISTRY_URL", "https://workers.internal");

        WorkerRegistryClient registryClient =
                new DefaultWorkerRegistryClient(HttpClient.newHttpClient(), registryUrl);
        TaskScheduler scheduler = new TaskScheduler();
        TaskDispatchService dispatchService = new TaskDispatchService(registryClient, scheduler);

        RunSummary summary = dispatchService.dispatchDueTasks(List.of(), Instant.now());
        System.out.printf("dispatched=%d degraded=%s%n", summary.dispatchedCount(), summary.degraded());
    }
}
