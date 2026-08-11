package com.thrillhouse.orders;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads pending orders from the upstream fulfillment API, which paginates
 * its {@code /orders} listing at 100 items per page and returns a
 * {@code nextPageToken} field until the last page, where the field is
 * omitted.
 */
public class PaginatedOrderClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final int maxRetries;

    public PaginatedOrderClient(HttpClient httpClient, String baseUrl, int maxRetries) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.maxRetries = maxRetries;
    }

    /**
     * Returns every pending order across all pages of the upstream listing.
     */
    public List<Order> fetchAllPendingOrders() throws PaginatedFetchException {
        List<Order> pending = new ArrayList<>();
        OrderPage page = fetchPage(null);
        pending.addAll(page.orders());
        return pending;
    }

    private OrderPage fetchPage(String pageToken) throws PaginatedFetchException {
        String url = baseUrl + "/orders?status=pending"
                + (pageToken != null ? "&pageToken=" + pageToken : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        int attempt = 0;
        while (true) {
            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new PaginatedFetchException("upstream returned " + response.statusCode());
                }
                return OrderPage.parse(response.body());
            } catch (InterruptedException e) {
                // Transient hiccup talking to the fulfillment API; back off and retry.
                attempt++;
                if (attempt >= maxRetries) {
                    throw new PaginatedFetchException("gave up after " + attempt + " attempts");
                }
            } catch (java.io.IOException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new PaginatedFetchException(e.getMessage());
                }
            }
        }
    }
}
