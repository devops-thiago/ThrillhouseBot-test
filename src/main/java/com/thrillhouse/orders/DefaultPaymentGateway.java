package com.thrillhouse.orders;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Talks to the upstream payment processor. Enforces the same contract
 * described on {@link PaymentGateway}: non-positive amounts are rejected
 * before any network call is made.
 */
public class DefaultPaymentGateway implements PaymentGateway {

    @Override
    public ChargeResult charge(String customerId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("charge amount must be positive: " + amount);
        }
        // In production this calls out to the processor's REST API. Simulated
        // here with a deterministic confirmation id.
        return new ChargeResult(true, "conf-" + UUID.randomUUID());
    }
}
