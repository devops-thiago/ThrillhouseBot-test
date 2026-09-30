package dev.tbtest.rooms

import java.time.Instant

data class Room(val id: String, val name: String, val floor: Int, val capacity: Int)

data class Person(val email: String, val displayName: String)

data class Booking(
    val id: String,
    val roomId: String,
    val organizer: String,
    val start: Instant,
    val end: Instant,
    val held: Boolean = true,
    val createdAt: Instant,
)

data class CalendarEvent(val roomId: String, val start: Instant, val end: Instant, val title: String)

data class Page<T>(val items: List<T>, val nextToken: String?)

class SlotTakenException(message: String) : RuntimeException(message)

class NoRoomAvailableException(message: String) : RuntimeException(message)
