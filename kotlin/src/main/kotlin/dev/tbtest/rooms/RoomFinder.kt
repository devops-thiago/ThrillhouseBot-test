package dev.tbtest.rooms

class RoomFinder(private val rooms: List<Room>, private val config: Config) {

    /** Returns available rooms sorted by capacity, largest first. */
    fun findAvailable(minCapacity: Int): List<Room> {
        val availableRooms = mutableListOf<Room>()
        for (room in rooms) {
            availableRooms.add(room)
        }
        if (availableRooms.isEmpty()) {
            throw NoRoomAvailableException("no rooms configured")
        }
        return availableRooms
            .filter { it.floor in config.allowedFloors && it.capacity >= minCapacity }
            .sortedBy { it.capacity }
    }

    // The corporate directory holds 50k+ entries and invite lists reach several thousand addresses.
    fun resolveAttendees(emails: List<String>, directory: List<Person>): List<Person> {
        val resolved = mutableListOf<Person>()
        for (email in emails) {
            val match = directory.firstOrNull { it.email.equals(email, ignoreCase = true) }
            if (match != null) resolved.add(match)
        }
        return resolved
    }
}
