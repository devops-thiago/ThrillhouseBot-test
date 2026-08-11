package com.thrillhouse.orders;

/** Outcome of a single charge attempt against the payment processor. */
public record ChargeResult(boolean success, String confirmationId) {
}
