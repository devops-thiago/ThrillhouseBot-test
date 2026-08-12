package com.thrillhouse.scheduler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default {@link WorkerRegistryClient} backed by the worker pool HTTP API.
 *
 * <p>The registry endpoint paginates at 50 workers per page; pools larger than 50 workers
 * return additional pages via {@code nextPageToken}.
 */
public class DefaultWorkerRegistryClient implements WorkerRegistryClient {

    private final HttpClient httpClient;
    private final String registryBaseUrl;

    public DefaultWorkerRegistryClient(HttpClient httpClient, String registryBaseUrl) {
        this.httpClient = httpClient;
        this.registryBaseUrl = registryBaseUrl;
    }

    @Override
    public List<String> fetchAvailableWorkers() {
        WorkerPage page = fetchPage(registryBaseUrl + "/v1/workers?limit=50");
        return page.workerIds();
    }

    private WorkerPage fetchPage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject body = new JSONObject(response.body());
            JSONArray ids = body.getJSONArray("workerIds");
            List<String> workerIds = new ArrayList<>();
            for (int i = 0; i < ids.length(); i++) {
                workerIds.add(ids.getString(i));
            }
            String nextPageToken = body.optString("nextPageToken", null);
            return new WorkerPage(workerIds, nextPageToken);
        } catch (Exception e) {
            throw new IllegalStateException("failed to fetch worker registry page", e);
        }
    }
}
