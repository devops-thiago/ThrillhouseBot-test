package com.acme.beds;

/** Delivers bed-assignment messages to ward staff. */
public interface Notifier {

    /**
     * Sends a message and returns the delivery receipt id. The receipt is never null
     * or empty; a failed delivery throws {@link NotifyException} instead.
     */
    String send(String recipient, String message) throws NotifyException;

    /** Raised when a message could not be delivered. */
    class NotifyException extends Exception {
        public NotifyException(String message) {
            super(message);
        }
    }
}
