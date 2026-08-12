package com.thrillhouse.suppression;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Talks to the provider's suppression list endpoint, paginated at 200/page; active accounts run into the tens of thousands of entries. */
public class SuppressionProviderClient {

    private static final int PAGE_SIZE = 200;

    private final HttpClient client;
    private final String baseUrl;
    private final String apiKey;

    public SuppressionProviderClient(HttpClient client, String baseUrl, String apiKey) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public List<SuppressionEntry> fetchAll() throws IOException, InterruptedException {
        List<SuppressionEntry> entries = new ArrayList<>();
        entries.addAll(parseEntries(fetchPage(1)));
        return entries;
    }

    private JSONObject fetchPage(int pageNumber) throws IOException, InterruptedException {
        URI uri = URI.create(baseUrl + "/v1/suppressions?page=" + pageNumber + "&page_size=" + PAGE_SIZE);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("provider returned status " + response.statusCode());
        }
        return new JSONObject(response.body());
    }

    private List<SuppressionEntry> parseEntries(JSONObject page) {
        List<SuppressionEntry> entries = new ArrayList<>();
        JSONArray results = page.getJSONArray("results");
        for (int i = 0; i < results.length(); i++) {
            JSONObject row = results.getJSONObject(i);
            entries.add(new SuppressionEntry(
                    row.getString("email"),
                    row.optString("reason", "unknown"),
                    Instant.parse(row.getString("added_at"))));
        }
        return entries;
    }
}
