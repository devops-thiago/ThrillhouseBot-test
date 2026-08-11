package com.thrillhouse.orders;

import java.math.BigDecimal;

/**
 * Charges a customer for an order.
 *
 * <p>Contract: {@link #charge} rejects non-positive amounts by throwing
 * {@link IllegalArgumentException} — it never silently succeeds for an
 * amount of zero or less. Callers must not pass unvalidated amounts.
 */
public interface PaymentGateway {

    ChargeResult charge(String customerId, BigDecimal amount);
}
