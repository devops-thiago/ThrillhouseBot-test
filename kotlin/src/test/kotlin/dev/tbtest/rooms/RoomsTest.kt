package dev.tbtest.rooms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class MutableClock(var now: Instant) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?): Clock = this
    override fun instant(): Instant = now
}

class FakeNotifier : Notifier {
    val sent = mutableListOf<String>()

    override fun send(recipient: String, message: String): Boolean {
        sent.add(recipient)
        return true
    }
}

class RoomsTest {
    private val t0 = Instant.parse("2026-01-05T09:00:00Z")
    private val config = Config(setOf(1, 2), Duration.ofMinutes(15), 50, "test-token")

    private fun at(h: Int): Instant = t0.plus(Duration.ofHours(h.toLong()))

    @Test
    fun `rejects an overlapping booking`() {
        val service = BookingService(config, FakeNotifier(), MutableClock(t0))
        service.book("r1", "a@example.com", at(0), at(2))
        assertThrows(SlotTakenException::class.java) {
            service.book("r1", "b@example.com", at(1), at(3))
        }
    }

    @Test
    fun `allows back-to-back bookings in the same room`() {
        val service = BookingService(config, FakeNotifier(), MutableClock(t0))
        service.book("r1", "a@example.com", at(0), at(1))
        val second = service.book("r1", "b@example.com", at(1), at(2))
        assertNotNull(second)
    }

    @Test
    fun `expires stale holds but keeps confirmed bookings`() {
        val clock = MutableClock(t0)
        val service = BookingService(config, FakeNotifier(), clock)
        val stale = service.book("r1", "a@example.com", at(0), at(1))
        val kept = service.book("r2", "b@example.com", at(0), at(1))
        service.confirm(kept.id)
        clock.now = t0.plus(Duration.ofMinutes(20))
        service.expireHolds()
        assertNull(service.get(stale.id))
        assertNotNull(service.get(kept.id))
    }

    @Test
    fun `notifies the organizer when a room is held`() {
        val notifier = FakeNotifier()
        val service = BookingService(config, notifier, MutableClock(t0))
        service.book("r1", " ", at(0), at(1))
        assertEquals(1, notifier.sent.size)
    }

    @Test
    fun `parses config from the environment`() {
        val parsed = Config.fromEnv(mapOf("ROOMS_ALLOWED_FLOORS" to "1, 3", "HOLD_TTL" to "60"))
        assertEquals(setOf(1, 3), parsed.allowedFloors)
        assertEquals(Duration.ofSeconds(60), parsed.holdTtl)
        assertEquals(50, parsed.calendarPageSize)
    }

    @Test
    fun `finds only rooms on allowed floors with enough seats`() {
        val rooms = listOf(Room("r1", "Fjord", 1, 6), Room("r2", "Atlas", 2, 12), Room("r3", "Summit", 3, 24))
        val found = RoomFinder(rooms, config).findAvailable(8)
        assertEquals(listOf("r2"), found.map { it.id })
    }

    @Test
    fun `resolves attendees case-insensitively`() {
        val directory = listOf(Person("ann@example.com", "Ann"), Person("bob@example.com", "Bob"))
        val out = RoomFinder(emptyList(), config).resolveAttendees(listOf("ANN@example.com", "zed@example.com"), directory)
        assertEquals(listOf("Ann"), out.map { it.displayName })
    }

    @Test
    fun `sync returns events from the calendar`() {
        val event = CalendarEvent("r1", at(0), at(1), "standup")
        val client = object : CalendarClient {
            override fun listEvents(roomId: String, pageToken: String?) = Page(listOf(event), null)
        }
        assertTrue(CalendarSync(client).busyEvents("r1").contains(event))
    }
}
