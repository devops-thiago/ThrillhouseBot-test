package bloodbank;

/** Temperature probe inside a storage fridge. */
public interface FridgeSensor {

    /** Thrown when the probe cannot be read. */
    class SensorOfflineException extends RuntimeException {
        public SensorOfflineException(String message) {
            super(message);
        }
    }

    /**
     * Returns the current temperature in degrees Celsius.
     *
     * @throws SensorOfflineException if the probe cannot be read; it never returns a sentinel value
     */
    double readCelsius();
}
