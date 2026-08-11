package com.thrillhouse.orders;

/** Raised when the upstream fulfillment API cannot be read. */
public class PaginatedFetchException extends Exception {

    public PaginatedFetchException(String message) {
        super(message);
    }
}
