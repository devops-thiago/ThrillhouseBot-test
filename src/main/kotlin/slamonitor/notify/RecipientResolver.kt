package slamonitor.notify

import slamonitor.evaluator.Breach
import slamonitor.model.Priority

/**
 * Decides who should be notified for a given batch of breaches: the
 * default recipients, plus the on-call managers when at least one
 * breach is for an URGENT-priority ticket. Returns an empty list when
 * there is nothing to report.
 */
interface RecipientResolver {
    fun resolve(breaches: List<Breach>, defaultRecipients: List<String>): List<String>
}

class PriorityAwareRecipientResolver(private val onCallManagers: List<String>) : RecipientResolver {

    override fun resolve(breaches: List<Breach>, defaultRecipients: List<String>): List<String> {
        if (breaches.isEmpty()) return emptyList()
        val hasUrgent = breaches.any { it.ticket.priority == Priority.URGENT }
        return if (hasUrgent) defaultRecipients + onCallManagers else defaultRecipients
    }
}
