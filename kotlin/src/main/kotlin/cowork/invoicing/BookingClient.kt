package cowork.invoicing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.YearMonth

/**
 * Client for the booking platform's REST API.
 *
 * `GET /v1/bookings?month=YYYY-MM&page_size=N` is cursor-paginated: each response carries at most
 * `page_size` bookings and a `nextPageToken`; pass it back as `page_token` to get the next page.
 * The last page has `nextPageToken = null`.
 */
class BookingClient(
    private val baseUrl: String,
    private val token: String,
    private val http: HttpClient = HttpClient.newHttpClient(),
) : BookingSource {
    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /** Returns every booking that overlaps [month]. */
    override fun bookingsFor(month: YearMonth): List<Booking> {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/v1/bookings?month=$month&page_size=200"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "booking platform returned ${response.statusCode()}" }
        val page: BookingPage = mapper.readValue(response.body())
        return page.items
    }
}
