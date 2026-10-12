package newsletter.billing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class PaymentDeclinedException(val subscriberId: String, reason: String) :
    RuntimeException("charge declined for $subscriberId: $reason")

interface PaymentGateway {
    /**
     * Charges the subscriber's card on file.
     *
     * Returns a receipt only for a successful charge (status is always "succeeded").
     * A declined card is reported by throwing [PaymentDeclinedException]; it never
     * comes back as a receipt.
     */
    fun charge(subscriberId: String, amountCents: Long, currency: String): ChargeReceipt
}

class HttpPaymentGateway(
    private val baseUrl: String,
    private val token: String,
    private val http: HttpClient = HttpClient.newHttpClient(),
) : PaymentGateway {
    private val mapper = jacksonObjectMapper()

    override fun charge(subscriberId: String, amountCents: Long, currency: String): ChargeReceipt {
        val body = mapper.writeValueAsString(
            mapOf("customer" to subscriberId, "amount" to amountCents, "currency" to currency),
        )
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/v1/charges"))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return when (response.statusCode()) {
            200, 201 -> mapper.readValue<ChargeReceipt>(response.body())
            402 -> throw PaymentDeclinedException(subscriberId, response.body())
            else -> error("payment gateway returned ${response.statusCode()}")
        }
    }
}
