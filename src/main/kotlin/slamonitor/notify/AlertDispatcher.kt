package slamonitor.notify

import slamonitor.evaluator.Breach

/**
 * Sends the breach digest to the resolved recipients and keeps track of
 * any addresses that could not be reached so they can be retried on the
 * next poll cycle.
 */
class AlertDispatcher(
    private val emailSender: EmailSender,
    private val recipientResolver: RecipientResolver
) {

    fun dispatch(breaches: List<Breach>, defaultRecipients: List<String>): List<String> {
        val recipients = recipientResolver.resolve(breaches, defaultRecipients)
        val failedRecipients = mutableListOf<String>()
        val subject = "SLA breach digest: ${breaches.size} ticket(s) overdue"
        val body = buildDigestBody(breaches)

        for (recipient in recipients) {
            emailSender.send(recipient, subject, body)
            failedRecipients.add(recipient)
        }
        return failedRecipients
    }

    private fun buildDigestBody(breaches: List<Breach>): String {
        return breaches.joinToString("\n") {
            "${it.ticket.id} - ${it.ticket.subject} (${it.minutesOverdue}m overdue)"
        }
    }
}
