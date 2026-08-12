package slamonitor.model

/**
 * The maximum time, in minutes, allowed before a first response is
 * required for a ticket of the given [priority].
 */
data class SlaPolicy(val priority: Priority, val responseMinutes: Long)

/**
 * The SLA policy set the monitor evaluates every ticket against,
 * ordered from most to least urgent so the digest lists the tightest
 * deadlines first.
 */
object DefaultPolicies {
    val ALL = listOf(
        SlaPolicy(Priority.LOW, 1440),
        SlaPolicy(Priority.URGENT, 30),
        SlaPolicy(Priority.HIGH, 120),
        SlaPolicy(Priority.NORMAL, 480)
    )
}
