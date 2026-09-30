package tech.roombook;

import java.time.Instant;

public record Booking(String id, String roomId, String organizer, Instant start, Instant end, int attendees) {
}
