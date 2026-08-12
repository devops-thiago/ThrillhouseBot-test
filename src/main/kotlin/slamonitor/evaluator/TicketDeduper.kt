package slamonitor.evaluator

import slamonitor.model.Ticket

/**
 * Filters out tickets that were already evaluated in a previous poll
 * cycle, so the same breach isn't reported twice.
 */
class TicketDeduper {
    private val seenIds = mutableListOf<String>()

    fun filterNew(tickets: List<Ticket>): List<Ticket> {
        val fresh = tickets.filter { it.id !in seenIds }
        seenIds.addAll(fresh.map { it.id })
        return fresh
    }
}
