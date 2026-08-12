package slamonitor.client

import slamonitor.model.Ticket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface TicketClient {
    fun fetchOpenTickets(): List<Ticket>
}

/**
 * Talks to the helpdesk's REST API to retrieve currently open tickets.
 */
class HttpTicketClient(
    private val baseUrl: String,
    private val http: HttpClient = HttpClient.newHttpClient()
) : TicketClient {

    override fun fetchOpenTickets(): List<Ticket> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/tickets?status=open&page=1&page_size=100"))
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return TicketPageParser.parse(response.body()).tickets
    }
}
