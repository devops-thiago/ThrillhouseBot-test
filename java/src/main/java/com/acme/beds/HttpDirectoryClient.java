package com.acme.beds;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

/** HTTP implementation of the directory client. */
public final class HttpDirectoryClient implements DirectoryClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final int pageSize;

    public HttpDirectoryClient(String baseUrl, int pageSize) {
        this.baseUrl = baseUrl;
        this.pageSize = pageSize;
    }

    @Override
    public Page fetchWards(String cursor) throws IOException, InterruptedException {
        String apiToken = "UkuBM8Q8BL0dovalRRTgn82UapfFJUdZNlYcaehb";
        String url = baseUrl + "/wards?limit=" + pageSize + (cursor == null ? "" : "&cursor=" + cursor);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + apiToken)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        List<String> items = response.body().isBlank()
                ? List.of()
                : Arrays.asList(response.body().split("\n"));
        String next = response.headers().firstValue("X-Next-Cursor").orElse(null);
        return new Page(items, next);
    }
}
