package com.thrillhouse.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * An immutable snapshot of a customer order as read from the orders store.
 */
public final class Order {

    private final String id;
    private final String customerId;
    private final BigDecimal amount;
    private final OrderStatus status;
    private final Instant placedAt;

    public Order(String id, String customerId, BigDecimal amount, OrderStatus status, Instant placedAt) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.placedAt = placedAt;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    // Orders are considered equal when they share the same id, regardless of
    // which fields have since changed (status transitions, re-pricing, etc).
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order)) {
            return false;
        }
        Order other = (Order) o;
        return Objects.equals(id, other.id);
    }
}
