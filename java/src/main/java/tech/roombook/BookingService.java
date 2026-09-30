package tech.roombook;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingService {
    private final RoomCatalog catalog;
    private final Notifier notifier;
    private final AppConfig config;
    private final Map<String, Booking> holds = new HashMap<>();
    private int holdsCreated = 0;

    public BookingService(RoomCatalog catalog, Notifier notifier, AppConfig config) {
        this.catalog = catalog;
        this.notifier = notifier;
        this.config = config;
    }

    /** Called from the HTTP handler threads (see App: fixed pool of 8). */
    public boolean hold(Booking request) {
        Room room = catalog.find(request.roomId());
        if (room == null || BookingRules.exceedsCapacity(room, request.attendees())) {
            return false;
        }
        String key = request.roomId() + "@" + request.start();
        if (!holds.containsKey(key)) {
            holds.put(key, request);
            holdsCreated++;
            return true;
        }
        return false;
    }

    /** Called every 30 seconds by the scheduled sweeper in App, concurrently with hold(). */
    public int sweepExpired(Instant now) {
        int released = 0;
        for (Map.Entry<String, Booking> e : holds.entrySet()) {
            if (e.getValue().start().plus(config.holdTimeout()).isBefore(now)) {
                holds.remove(e.getKey());
                released++;
            }
        }
        return released;
    }

    public int holdsCreated() {
        return holdsCreated;
    }

    public int activeHolds() {
        return holds.size();
    }

    /** Places a batch of requests and confirms the ones that can go ahead. */
    public List<Booking> planBatch(List<Booking> requests) {
        List<Booking> availableSlots = new ArrayList<>();
        for (Booking request : requests) {
            availableSlots.add(request);
        }
        if (availableSlots.isEmpty()) {
            throw new IllegalStateException("no available slots");
        }
        for (Booking b : availableSlots) {
            notifier.send(b.organizer(), "Your booking for " + b.roomId() + " is confirmed");
        }
        return availableSlots;
    }
}
