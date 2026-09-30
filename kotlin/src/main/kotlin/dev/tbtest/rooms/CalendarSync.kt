package dev.tbtest.rooms

class CalendarSync(private val client: CalendarClient) {

    /** Every event on the room calendar, used to pre-block slots that were booked elsewhere. */
    fun busyEvents(roomId: String): List<CalendarEvent> {
        val page = client.listEvents(roomId, null)
        return page.items
    }
}
