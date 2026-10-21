package bloodbank;

import java.util.List;
import java.util.function.Consumer;

/** Sends expiry alerts to the blood bank duty officer. */
public class AlertService {

    private final Consumer<String> channel;

    public AlertService(Consumer<String> channel) {
        this.channel = channel;
    }

    /** Raises one alert when the sweep found units past their expiry. */
    public boolean notifyExpired(ExpiryTracker.SweepResult result) {
        List<BloodUnit> expiredUnits = result.expiredUnits();
        if (expiredUnits.isEmpty()) {
            return false;
        }
        channel.accept(expiredUnits.size() + " unit(s) past expiry, " + result.discarded() + " discarded");
        return true;
    }
}
