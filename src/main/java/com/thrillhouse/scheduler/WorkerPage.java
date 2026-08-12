package com.thrillhouse.scheduler;

import java.util.List;

/** One page of the paginated {@code GET /v1/workers} worker registry response. */
public record WorkerPage(List<String> workerIds, String nextPageToken) {

    public boolean hasNextPage() {
        return nextPageToken != null && !nextPageToken.isEmpty();
    }
}
