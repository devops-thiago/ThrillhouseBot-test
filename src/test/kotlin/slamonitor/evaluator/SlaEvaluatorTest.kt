package slamonitor.evaluator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import slamonitor.model.Priority
import slamonitor.model.SlaPolicy
import slamonitor.model.Ticket
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private val FIXED_NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")
private val FIXED_CLOCK: Clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)

private val POLICIES = listOf(
    SlaPolicy(Priority.URGENT, 30),
    SlaPolicy(Priority.HIGH, 120)
)

private fun ticket(id: String, priority: Priority, ageMinutes: Long, firstResponseAt: Long? = null) = Ticket(
    id = id,
    subject = "subject-$id",
    customerEmail = "customer@example.com",
    priority = priority,
    createdAt = FIXED_NOW.minusSeconds(ageMinutes * 60).toEpochMilli(),
    firstResponseAt = firstResponseAt
)

class SlaEvaluatorTest {

    @Test
    fun `flags a ticket that has been open longer than its priority allows`() {
        val evaluator = SlaEvaluator(POLICIES, FIXED_CLOCK)
        val tickets = listOf(ticket("1", Priority.URGENT, ageMinutes = 45))

        val breaches = evaluator.findBreaches(tickets)

        assertEquals(1, breaches.size)
        assertEquals("1", breaches[0].ticket.id)
        assertEquals(15, breaches[0].minutesOverdue)
    }

    @Test
    fun `does not flag a ticket that already received a first response`() {
        val evaluator = SlaEvaluator(POLICIES, FIXED_CLOCK)
        val tickets = listOf(
            ticket("1", Priority.URGENT, ageMinutes = 90, firstResponseAt = FIXED_NOW.toEpochMilli())
        )

        val breaches = evaluator.findBreaches(tickets)

        assertTrue(breaches.isEmpty())
    }

    @Test
    fun `mostOverdue returns all breaches when there are fewer than the limit`() {
        val evaluator = SlaEvaluator(POLICIES, FIXED_CLOCK)
        val breaches = listOf(
            ticket("1", Priority.URGENT, ageMinutes = 45),
            ticket("2", Priority.HIGH, ageMinutes = 150)
        ).let { evaluator.findBreaches(it) }

        val topBreaches = evaluator.mostOverdue(breaches, limit = 10)

        assertEquals(2, topBreaches.size)
    }
}
