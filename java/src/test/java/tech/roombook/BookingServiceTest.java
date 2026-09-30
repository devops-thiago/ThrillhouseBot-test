package tech.roombook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingServiceTest {
    private static final Instant T0 = Instant.parse("2026-01-05T09:00:00Z");

    private final List<String> sent = new ArrayList<>();
    // Stub accepts any address and always answers with a receipt.
    private final Notifier notifier = (address, message) -> {
        sent.add(address);
        return "ok";
    };

    private BookingService service() {
        RoomDirectory dir = page -> new RoomDirectory.Page(List.of(new Room("R1", 10)), page, false);
        AppConfig config = new AppConfig(List.of("example.com"), Duration.ofMinutes(15), 50);
        return new BookingService(new RoomCatalog(dir), notifier, config);
    }

    private static Booking booking(String room, int attendees, Instant start) {
        return new Booking("b", room, "ann@example.com", start, start.plusSeconds(3600), attendees);
    }

    @Test
    void holdsRoomWithinCapacity() {
        assertTrue(service().hold(booking("R1", 5, T0)));
    }

    @Test
    void allowsExactlyFullRoom() {
        assertTrue(service().hold(booking("R1", 10, T0)));
    }

    @Test
    void rejectsOverCapacity() {
        assertFalse(service().hold(booking("R1", 11, T0)));
    }

    @Test
    void rejectsDoubleHold() {
        BookingService s = service();
        assertTrue(s.hold(booking("R1", 2, T0)));
        assertFalse(s.hold(booking("R1", 2, T0)));
    }

    @Test
    void sweepReleasesExpiredHolds() {
        BookingService s = service();
        s.hold(booking("R1", 2, T0));
        assertEquals(1, s.sweepExpired(T0.plus(Duration.ofMinutes(30))));
    }

    @Test
    void batchNotifiesOrganizerWithMalformedAddress() {
        BookingService s = service();
        Booking b = new Booking("x", "R1", "not-an-address", T0, T0.plusSeconds(600), 2);
        s.planBatch(List.of(b));
        assertEquals(List.of("not-an-address"), sent);
    }

    @Test
    void detectsOverlaps() {
        Booking a = booking("R1", 2, T0);
        Booking b = booking("R1", 2, T0.plusSeconds(1800));
        assertEquals(1, BookingRules.countOverlaps(List.of(a, b)));
    }
}
