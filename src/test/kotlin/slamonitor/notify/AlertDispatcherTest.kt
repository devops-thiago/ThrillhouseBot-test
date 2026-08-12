package slamonitor.notify

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import slamonitor.evaluator.Breach
import slamonitor.model.Priority
import slamonitor.model.Ticket

private class StubRecipientResolver(private val recipients: List<String>) : RecipientResolver {
    override fun resolve(breaches: List<Breach>, defaultRecipients: List<String>): List<String> = recipients
}

private class RecordingEmailSender : EmailSender {
    val sentTo = mutableListOf<String>()
    override fun send(to: String, subject: String, body: String): Boolean {
        sentTo.add(to)
        return true
    }
}

private fun sampleBreach() = Breach(
    ticket = Ticket(
        id = "T-1",
        subject = "Payments API returning 500s",
        customerEmail = "customer@example.com",
        priority = Priority.URGENT,
        createdAt = 0L
    ),
    minutesOverdue = 12
)

class AlertDispatcherTest {

    @Test
    fun `dispatch emails every resolved recipient`() {
        val sender = RecordingEmailSender()
        val dispatcher = AlertDispatcher(sender, StubRecipientResolver(listOf("team@example.com")))

        dispatcher.dispatch(listOf(sampleBreach()), listOf("team@example.com"))

        assertEquals(listOf("team@example.com"), sender.sentTo)
    }
}
