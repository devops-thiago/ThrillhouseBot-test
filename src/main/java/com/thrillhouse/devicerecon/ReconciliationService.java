package com.thrillhouse.devicerecon;

import com.thrillhouse.devicerecon.RetirementPolicy.Action;
import com.thrillhouse.devicerecon.RetirementPolicy.Decision;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reconciles the MDM's enrolments against the directory and retires what policy allows. */
public class ReconciliationService {

    /** What the run decided about one device. */
    public record Outcome(String deviceId, String serialNumber, String ownerEmail, Action action, String reason) {}

    /** The result of one run, as returned by the HTTP endpoint and logged by the scheduled run. */
    public record ReconciliationReport(
            Instant startedAt,
            int devicesExamined,
            int retired,
            int kept,
            boolean dryRun,
            List<Outcome> needsReview,
            List<Outcome> failures) {}

    private static final Logger LOG = Logger.getLogger(ReconciliationService.class.getName());

    private final MdmClient mdm;
    private final DirectoryClient directory;
    private final DeviceInventory inventory;
    private final RetirementPolicy policy;
    private final Clock clock;
    private final boolean dryRun;
    private volatile ReconciliationReport lastReport;

    public ReconciliationService(MdmClient mdm, DirectoryClient directory, DeviceInventory inventory,
            RetirementPolicy policy, Clock clock, boolean dryRun) {
        this.mdm = mdm;
        this.directory = directory;
        this.inventory = inventory;
        this.policy = policy;
        this.clock = clock;
        this.dryRun = dryRun;
    }

    /**
     * Runs one reconciliation: pull the enrolments, refresh the mirror, look the owners up in the
     * directory and act on each device.
     *
     * <p>A device whose retirement fails is reported rather than retried here. The run happens on a
     * schedule, so the next one picks it up, and a device that the MDM refuses to retire twice in a
     * row is something an operator should see.
     */
    public ReconciliationReport reconcile() throws IOException, InterruptedException, SQLException {
        Instant startedAt = clock.instant();
        List<EnrolledDevice> devices = mdm.listEnrolledDevices();
        inventory.recordSeen(devices, startedAt);

        Map<String, DirectoryAccount> accounts = directory.lookup(ownerAddresses(devices));
        List<Outcome> needsReview = new ArrayList<>();
        List<Outcome> failures = new ArrayList<>();
        int retired = 0;
        int kept = 0;

        for (EnrolledDevice device : devices) {
            Decision decision = policy.evaluate(device, accounts.get(device.ownerKey()), startedAt);
            switch (decision.action()) {
                case KEEP -> kept++;
                case REVIEW -> needsReview.add(outcome(device, decision));
                case RETIRE -> {
                    if (retire(device, decision)) {
                        retired++;
                    } else {
                        failures.add(outcome(device, decision));
                    }
                }
            }
        }

        int examined = devices.size();
        int retiredCount = retired;
        LOG.info(() -> "reconciled " + examined + " devices: " + retiredCount + " retired, "
                + needsReview.size() + " for review, " + failures.size() + " failed");
        lastReport = new ReconciliationReport(startedAt, examined, retired, kept, dryRun, needsReview, failures);
        return lastReport;
    }

    /** The most recent report, or null before the first run has finished. */
    public ReconciliationReport lastReport() {
        return lastReport;
    }

    /** The distinct owner addresses to look up, in the order the devices were listed. */
    Set<String> ownerAddresses(List<EnrolledDevice> devices) {
        Set<String> addresses = new LinkedHashSet<>();
        for (EnrolledDevice device : devices) {
            String key = device.ownerKey();
            if (!key.isEmpty()) {
                addresses.add(key);
            }
        }
        return addresses;
    }

    private boolean retire(EnrolledDevice device, Decision decision) throws InterruptedException {
        if (dryRun) {
            LOG.info(() -> "dry run: would retire " + device.serialNumber() + " (" + decision.reason() + ")");
            return true;
        }
        try {
            mdm.retireDevice(device.id(), decision.reason());
            inventory.markRetired(device.id(), clock.instant(), decision.reason());
            return true;
        } catch (IOException | SQLException failure) {
            LOG.log(Level.WARNING, failure, () -> "could not retire device " + device.id());
            return false;
        }
    }

    private static Outcome outcome(EnrolledDevice device, Decision decision) {
        return new Outcome(device.id(), device.serialNumber(), device.ownerKey(),
                decision.action(), decision.reason());
    }
}
