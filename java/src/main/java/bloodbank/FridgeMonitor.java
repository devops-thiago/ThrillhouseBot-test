package bloodbank;

/** Checks that a storage fridge is inside the 1-6 C band required for red cells. */
public class FridgeMonitor {

    public enum Status { OK, OUT_OF_RANGE, OFFLINE }

    private final FridgeSensor sensor;

    public FridgeMonitor(FridgeSensor sensor) {
        this.sensor = sensor;
    }

    public Status check() {
        double celsius;
        try {
            celsius = sensor.readCelsius();
        } catch (FridgeSensor.SensorOfflineException e) {
            return Status.OFFLINE;
        }
        return celsius >= 1.0 && celsius <= 6.0 ? Status.OK : Status.OUT_OF_RANGE;
    }
}
