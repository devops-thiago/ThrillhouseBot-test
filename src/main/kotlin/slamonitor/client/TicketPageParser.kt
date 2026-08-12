package slamonitor.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import slamonitor.model.Agent
import slamonitor.model.Priority
import slamonitor.model.Ticket

/**
 * One page of the helpdesk's `/tickets` response. [nextPage] carries the
 * cursor for the following page, or null once the last page is reached.
 */
data class TicketPage(val tickets: List<Ticket>, val nextPage: Int?)

private data class RawAgent(val id: String, val name: String, val email: String)

private data class RawTicket(
    val id: String,
    val subject: String,
    val customer_email: String,
    val priority: String,
    val created_at: Long,
    val first_response_at: Long?,
    val assigned_agent: RawAgent?
)

private data class RawPage(val tickets: List<RawTicket>, val next_page: Int?)

object TicketPageParser {
    private val mapper = jacksonObjectMapper()

    fun parse(body: String): TicketPage {
        val raw: RawPage = mapper.readValue(body)
        val tickets = raw.tickets.map {
            Ticket(
                id = it.id,
                subject = it.subject,
                customerEmail = it.customer_email,
                priority = Priority.valueOf(it.priority.uppercase()),
                createdAt = it.created_at,
                firstResponseAt = it.first_response_at,
                assignedAgent = it.assigned_agent?.let { raw -> Agent(raw.id, raw.name, raw.email) }
            )
        }
        return TicketPage(tickets, raw.next_page)
    }
}
