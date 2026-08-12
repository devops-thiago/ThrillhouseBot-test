package com.thrillhouse.suppression;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

/** Sends sync counters to the metrics backend over HTTP. */
public class HttpSyncMetricsPublisher implements SyncMetricsPublisher {

    private final HttpClient client;
    private final String metricsUrl;

    public HttpSyncMetricsPublisher(HttpClient client, String metricsUrl) {
        this.client = client;
        this.metricsUrl = metricsUrl;
    }

    @Override
    public PublishOutcome publish(SyncStats stats) {
        JSONObject body = new JSONObject();
        body.put("fetched", stats.fetched());
        body.put("filtered", stats.filtered());
        body.put("delivered", stats.delivered());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metricsUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() / 100 == 2 ? PublishOutcome.SUCCESS : PublishOutcome.FAILED;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return PublishOutcome.FAILED;
        }
    }
}
