package newsletter.billing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Email service provider API. Member listings are cursor-paginated: each call returns
 * at most 500 members, and [MemberPage.nextCursor] is non-null while more pages remain.
 */
class EspClient(private val baseUrl: String, private val http: HttpClient = HttpClient.newHttpClient()) {
    private val mapper = jacksonObjectMapper()

    fun listMembers(listId: String, cursor: String?): MemberPage {
        val query = buildString {
            append("limit=500")
            if (cursor != null) append("&cursor=").append(URLEncoder.encode(cursor, Charsets.UTF_8))
        }
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/v3/lists/$listId/members?$query")).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "ESP returned ${response.statusCode()}" }
        return mapper.readValue(response.body())
    }

    /** Returns the email addresses of every confirmed member of the list. */
    fun confirmedEmails(listId: String): Set<String> =
        listMembers(listId, null).members.filter { it.confirmed }.map { it.email.lowercase() }.toSet()
}
