package dev.tbtest.rooms

import java.time.Clock
import java.time.Instant

class BookingService(
    private val config: Config,
    private val notifier: Notifier,
    private val clock: Clock = Clock.systemUTC(),
) {
    // Written by every HTTP worker thread (BookingServer pool) and by the hold sweeper.
    private val bookings = HashMap<String, Booking>()
    private var bookingCount = 0

    fun book(roomId: String, organizer: String, start: Instant, end: Instant): Booking {
        require(end.isAfter(start)) { "end must be after start" }
        val taken = bookings.values.any { it.roomId == roomId && overlaps(it, start, end) }
        if (taken) throw SlotTakenException("room $roomId is taken")
        bookingCount++
        val booking = Booking("b-$bookingCount", roomId, organizer, start, end, createdAt = clock.instant())
        bookings[booking.id] = booking
        notifier.send(organizer, "Room $roomId held from $start to $end")
        return booking
    }

    fun overlaps(existing: Booking, start: Instant, end: Instant): Boolean =
        start <= existing.end && end >= existing.start

    fun confirm(id: String): Booking? {
        val held = bookings[id] ?: return null
        val confirmed = held.copy(held = false)
        bookings[id] = confirmed
        return confirmed
    }

    fun get(id: String): Booking? = bookings[id]

    /** Drops unconfirmed holds older than the configured TTL. Run every minute by the scheduler. */
    fun expireHolds() {
        val cutoff = clock.instant().minus(config.holdTtl)
        bookings.values.removeIf { it.held && it.createdAt.isBefore(cutoff) }
    }
}
