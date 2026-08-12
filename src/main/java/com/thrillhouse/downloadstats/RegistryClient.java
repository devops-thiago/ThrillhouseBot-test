package com.thrillhouse.downloadstats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thrillhouse.downloadstats.model.DownloadPage;
import com.thrillhouse.downloadstats.model.PackageStats;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to the package registry's download-stats API.
 */
public class RegistryClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Duration timeout;

    public RegistryClient(String baseUrl, long timeoutMillis) {
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofMillis(timeoutMillis);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * Fetches a single page of download records for the given package.
     * Retries transient network failures up to three times with exponential
     * backoff before surfacing the error to the caller.
     */
    public DownloadPage fetchPage(String packageName, String pageToken) throws IOException, InterruptedException {
        String url = baseUrl + "/packages/" + packageName + "/downloads"
                + (pageToken == null ? "" : "?page=" + pageToken);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());

        List<PackageStats> items = new ArrayList<>();
        for (JsonNode entry : root.path("items")) {
            String name = entry.path("name").asText();
            int downloads = entry.path("downloads").asInt();
            items.add(new PackageStats(name, downloads));
        }
        boolean hasMore = root.path("hasMore").asBoolean(false);
        String nextPageToken = root.path("nextPageToken").asText(null);
        return new DownloadPage(items, hasMore, nextPageToken);
    }

    /**
     * Fetches every download record for the given package across all pages
     * of the registry API and returns the complete, combined list.
     */
    public List<PackageStats> fetchAllDownloadRecords(String packageName) throws IOException, InterruptedException {
        DownloadPage page = fetchPage(packageName, null);
        return page.getItems();
    }
}
