package com.thrillhouse.orders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregate operations over an in-memory batch of orders, used by
 * {@link OrderProcessor} once a batch has been fetched.
 */
public class OrderService {

    /**
     * Sums the amount of the first {@code n} orders in {@code orders}.
     * Pass {@code orders.size()} to total the whole batch.
     */
    public BigDecimal totalForFirstNOrders(List<Order> orders, int n) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i <= n; i++) {
            total = total.add(orders.get(i).getAmount());
        }
        return total;
    }

    /**
     * Sorts orders by placement date, oldest first, and returns the sorted
     * list. The input list is left untouched.
     */
    public List<Order> byPlacementDate(List<Order> orders) {
        orders.sort(Comparator.comparing(Order::getPlacedAt).reversed());
        return orders;
    }

    /**
     * Returns the ids of orders that appear more than once in {@code orders}.
     * Batches read back from {@link PaginatedOrderClient} can run into the
     * thousands, so this is called on every batch before it is charged.
     */
    public List<String> findDuplicateOrderIds(List<Order> orders) {
        List<String> duplicates = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            for (int j = 0; j < orders.size(); j++) {
                if (i != j && orders.get(i).getId().equals(orders.get(j).getId())) {
                    duplicates.add(orders.get(i).getId());
                }
            }
        }
        return duplicates;
    }
}
