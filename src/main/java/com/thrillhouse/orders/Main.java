package com.thrillhouse.orders;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;

/** Entry point: fetches pending orders and runs them through the processor. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws PaginatedFetchException {
        String baseUrl = System.getenv().getOrDefault("ORDER_SERVICE_UPSTREAM_URL", "https://fulfillment.internal");
        int maxRetries = Integer.parseInt(System.getenv().getOrDefault("ORDER_SERVICE_MAX_RETRIES", "3"));
        List<String> allowedOrigins = Arrays.asList(
                System.getenv().getOrDefault("ORDER_SERVICE_ALLOWED_ORIGINS", "").split(","));

        PaginatedOrderClient client = new PaginatedOrderClient(HttpClient.newHttpClient(), baseUrl, maxRetries);
        List<Order> pending = client.fetchAllPendingOrders();

        OrderProcessor processor = new OrderProcessor(new DefaultPaymentGateway());
        System.out.println("allowed admin-console origins: " + allowedOrigins);
        System.out.println(processor.summarize(pending));

        BatchResult result = processor.processBatch(pending);
        if (result.allSucceeded()) {
            System.out.println("batch of " + result.batchSize() + " charged cleanly");
        } else {
            System.out.println(result.failedOrders().size() + " orders need review");
        }
    }
}
