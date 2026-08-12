package slamonitor.model

enum class Priority { LOW, NORMAL, HIGH, URGENT }

data class Agent(val id: String, val name: String, val email: String)

/**
 * A single support ticket pulled from the helpdesk.
 *
 * [firstResponseAt] is null until an agent posts the first reply, and
 * [assignedAgent] is null for tickets still sitting in the unassigned
 * queue.
 */
data class Ticket(
    val id: String,
    val subject: String,
    val customerEmail: String,
    val priority: Priority,
    val createdAt: Long,
    val firstResponseAt: Long? = null,
    val assignedAgent: Agent? = null
)
