package com.thrillhouse.orders;

import java.util.List;

/** Outcome of processing one batch of orders. */
public record BatchResult(int batchSize, boolean allSucceeded, List<Order> failedOrders) {
}
