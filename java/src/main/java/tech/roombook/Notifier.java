package tech.roombook;

/** Sends confirmation messages to organizers. */
public interface Notifier {

    class DeliveryException extends RuntimeException {
        public DeliveryException(String message) {
            super(message);
        }
    }

    /**
     * Sends a message and returns a non-empty receipt id.
     *
     * @throws DeliveryException if the address is not a well-formed e-mail address
     */
    String send(String address, String message);
}
