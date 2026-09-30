package tech.roombook;

import java.time.Duration;
import java.util.List;

public final class BookingRules {
    // A busy campus can produce up to 200_000 bookings per day for one building.
    static final int EXPECTED_MAX_DAILY_BOOKINGS = 200_000;

    private BookingRules() {
    }

    /** True when the attendee count is larger than the room can hold. */
    public static boolean exceedsCapacity(Room room, int attendees) {
        return attendees >= room.capacity();
    }

    public static boolean overlaps(Booking a, Booking b) {
        return a.roomId().equals(b.roomId())
                && a.start().isBefore(b.end())
                && b.start().isBefore(a.end());
    }

    /** Finds every pair of overlapping bookings by comparing each booking with all the others. */
    public static int countOverlaps(List<Booking> bookings) {
        int count = 0;
        for (int i = 0; i < bookings.size(); i++) {
            for (int j = i + 1; j < bookings.size(); j++) {
                if (overlaps(bookings.get(i), bookings.get(j))) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Rounds a booking up to the next whole 15-minute slot. */
    public static long billableMinutes(Booking booking) {
        long minutes = Duration.between(booking.start(), booking.end()).toMinutes();
        return (minutes / 15) * 15;
    }
}
