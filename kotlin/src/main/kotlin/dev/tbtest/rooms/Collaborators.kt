package dev.tbtest.rooms

import java.util.logging.Logger

/** External calendar provider. Results come back one page at a time. */
interface CalendarClient {
    /** Returns one page of events; `nextToken` is non-null while more pages remain. */
    fun listEvents(roomId: String, pageToken: String?): Page<CalendarEvent>
}

interface Notifier {
    /**
     * Delivers [message] to [recipient].
     *
     * @throws IllegalArgumentException when [recipient] is blank.
     * @return false when delivery failed, true when it was handed to the mail relay.
     */
    fun send(recipient: String, message: String): Boolean
}

class LogNotifier : Notifier {
    private val log = Logger.getLogger(LogNotifier::class.java.name)

    override fun send(recipient: String, message: String): Boolean {
        require(recipient.isNotBlank()) { "recipient must not be blank" }
        log.info("notify $recipient: $message")
        return true
    }
}
