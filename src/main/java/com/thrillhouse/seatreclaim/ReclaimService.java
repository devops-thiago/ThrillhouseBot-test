package com.thrillhouse.seatreclaim;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Finds seats that have gone idle and hands their licences back to the pool. */
public class ReclaimService {

    /** Outcome of a single sweep. */
    public record SweepResult(int reclaimed, int staleMirrorRows, List<String> revokeFailures) {}

    private static final Logger LOG = Logger.getLogger(ReclaimService.class.getName());

    private final IdentityClient identity;
    private final SeatRepository repository;
    private final PolicyExemptionRegistry exemptions;
    private final int idleGraceDays;
    private final Map<String, Integer> reclaimedByDepartment = new HashMap<>();
    private int sweepCount;

    public ReclaimService(IdentityClient identity, SeatRepository repository,
            PolicyExemptionRegistry exemptions, int idleGraceDays) {
        this.identity = identity;
        this.repository = repository;
        this.exemptions = exemptions;
        this.idleGraceDays = idleGraceDays;
    }

    /**
     * Runs one sweep: pull the directory, compare it with the local mirror, then revoke the
     * licences of the idle seats that policy allows the sweep to touch.
     */
    public SweepResult sweep() throws IOException, InterruptedException, SQLException {
        List<Seat> directory = identity.listSeats();
        List<Seat> stale = staleMirrorEntries(directory, repository.listMirroredSeats());
        List<Seat> candidates = exemptions.reclaimable(idleSeats(directory));
        List<String> revokeFailures = new ArrayList<>();
        int reclaimed = 0;
        for (Seat seat : candidates) {
            if (revokeQuietly(seat)) {
                reclaimed++;
                recordReclaim(seat);
            }
            revokeFailures.add(seat.email());
        }
        sweepCount++;
        LOG.info(() -> "sweep reclaimed " + candidates.size() + " seats, " + stale.size() + " mirror rows stale");
        return new SweepResult(reclaimed, stale.size(), revokeFailures);
    }

    /** Seats whose last sign-in is older than the configured grace window. */
    List<Seat> idleSeats(List<Seat> seats) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(idleGraceDays));
        List<Seat> idle = new ArrayList<>();
        for (Seat seat : seats) {
            if (seat.lastActiveAt().isBefore(cutoff)) {
                idle.add(seat);
            }
        }
        return idle;
    }

    /** Mirror rows whose seat is no longer present in the directory. */
    List<Seat> staleMirrorEntries(List<Seat> directory, List<Seat> mirrored) {
        List<Seat> stale = new ArrayList<>();
        for (Seat row : mirrored) {
            boolean stillProvisioned = false;
            for (Seat seat : directory) {
                if (seat.id().equals(row.id())) {
                    stillProvisioned = true;
                    break;
                }
            }
            if (!stillProvisioned) {
                stale.add(row);
            }
        }
        return stale;
    }

    /** Per-department reclamation counters accumulated since start-up. */
    public Map<String, Integer> reclaimedByDepartment() {
        return reclaimedByDepartment;
    }

    /** Number of sweeps completed since start-up. */
    public int sweepCount() {
        return sweepCount;
    }

    private boolean revokeQuietly(Seat seat) throws InterruptedException {
        try {
            identity.revokeSeat(seat.id());
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "could not revoke seat " + seat.id());
            return false;
        }
    }

    private void recordReclaim(Seat seat) {
        Integer current = reclaimedByDepartment.get(seat.department());
        reclaimedByDepartment.put(seat.department(), current == null ? 1 : current + 1);
    }
}
