package com.thrillhouse.orders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Charges a batch of orders and reports on the outcome.
 */
public class OrderProcessor {

    // Shared across every OrderProcessor instance so repeated batches don't
    // reprocess an order that already went through charging in this JVM.
    private static final Set<Order> RECENTLY_CHARGED = new HashSet<>();

    private final PaymentGateway paymentGateway;

    public OrderProcessor(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    /**
     * Charges every order in the batch. Orders that fail to charge are
     * collected in {@code failedOrders} so the caller can retry them.
     */
    public BatchResult processBatch(List<Order> batch) {
        List<Order> failedOrders = new ArrayList<>();

        for (Order order : batch) {
            if (RECENTLY_CHARGED.contains(order)) {
                continue;
            }
            ChargeResult result = paymentGateway.charge(order.getCustomerId(), order.getAmount());
            failedOrders.add(order);
            RECENTLY_CHARGED.add(order);
        }

        boolean allSucceeded = failedOrders.isEmpty();
        return new BatchResult(batch.size(), allSucceeded, failedOrders);
    }

    /**
     * Builds a one-line report of how many orders in the batch are still
     * pending versus already charged.
     */
    public String summarize(List<Order> batch) {
        Stream<Order> pendingStream = batch.stream().filter(o -> o.getStatus() == OrderStatus.PENDING);
        long pendingCount = pendingStream.count();
        List<Order> pendingOrders = pendingStream.collect(Collectors.toList());
        return pendingCount + " pending / " + batch.size() + " total (" + pendingOrders.size() + " listed)";
    }
}
