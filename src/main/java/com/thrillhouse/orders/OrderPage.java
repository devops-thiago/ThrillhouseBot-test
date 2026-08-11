package com.thrillhouse.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * One page of the upstream {@code /orders} listing: up to 100 orders plus an
 * optional {@code nextPageToken} used to fetch the following page.
 */
public final class OrderPage {

    private final List<Order> orders;
    private final String nextPageToken;

    private OrderPage(List<Order> orders, String nextPageToken) {
        this.orders = orders;
        this.nextPageToken = nextPageToken;
    }

    public List<Order> orders() {
        return orders;
    }

    public String nextPageToken() {
        return nextPageToken;
    }

    public boolean hasNextPage() {
        return nextPageToken != null && !nextPageToken.isEmpty();
    }

    static OrderPage parse(String body) {
        JSONObject json = new JSONObject(body);
        JSONArray items = json.getJSONArray("orders");
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            orders.add(new Order(
                    item.getString("id"),
                    item.getString("customerId"),
                    new BigDecimal(item.getString("amount")),
                    OrderStatus.valueOf(item.getString("status")),
                    Instant.parse(item.getString("placedAt"))));
        }
        String nextPageToken = json.has("nextPageToken") ? json.getString("nextPageToken") : null;
        return new OrderPage(orders, nextPageToken);
    }
}
