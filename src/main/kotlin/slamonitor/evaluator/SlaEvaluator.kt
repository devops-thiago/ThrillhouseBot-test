package slamonitor.evaluator

import slamonitor.model.SlaPolicy
import slamonitor.model.Ticket
import java.time.Clock

data class Breach(val ticket: Ticket, val minutesOverdue: Long)

/**
 * Compares open tickets against their priority's SLA policy and reports
 * the ones that have breached first-response time. Tickets that have
 * already received a first response are excluded, since the SLA clock
 * stops the moment an agent replies.
 */
class SlaEvaluator(
    private val policies: List<SlaPolicy>,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findBreaches(tickets: List<Ticket>): List<Breach> {
        val breaches = mutableListOf<Breach>()
        val now = clock.millis()

        for (ticket in tickets) {
            if (ticket.firstResponseAt != null) continue

            val policy = policies.find { it.priority == ticket.priority } ?: continue
            val elapsedMinutes = (now - ticket.createdAt) / 60_000
            if (elapsedMinutes > policy.responseMinutes) {
                breaches.add(Breach(ticket, elapsedMinutes - policy.responseMinutes))
            }
        }
        return breaches
    }

    /**
     * Returns the [limit] most-overdue breaches, most overdue first, for
     * inclusion in the alert digest.
     */
    fun mostOverdue(breaches: List<Breach>, limit: Int): List<Breach> {
        val sorted = breaches.sortedByDescending { it.minutesOverdue }
        return sorted.subList(0, minOf(limit - 1, sorted.size))
    }
}
