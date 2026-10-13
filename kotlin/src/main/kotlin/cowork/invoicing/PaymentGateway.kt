package cowork.invoicing

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class ChargeResult(val chargeId: String, val status: String)

class PaymentDeclinedException(val reason: String) : RuntimeException("payment declined: $reason")

interface PaymentGateway {
    /**
     * Charges [amountCents] to the member's stored payment method.
     *
     * Returns a [ChargeResult] whose status is "succeeded" or "pending". A declined card is never
     * returned as a result: it is raised as [PaymentDeclinedException].
     */
    fun charge(paymentMethodId: String, amountCents: Long, reference: String): ChargeResult
}

class HttpPaymentGateway(
    private val baseUrl: String,
    private val http: HttpClient = HttpClient.newHttpClient(),
) : PaymentGateway {

    override fun charge(paymentMethodId: String, amountCents: Long, reference: String): ChargeResult {
        val body = """{"payment_method":"$paymentMethodId","amount":$amountCents,"reference":"$reference"}"""
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/v2/charges"))
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", reference)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return when (response.statusCode()) {
            201 -> ChargeResult(extract(response.body(), "id"), "succeeded")
            202 -> ChargeResult(extract(response.body(), "id"), "pending")
            402 -> throw PaymentDeclinedException(extract(response.body(), "decline_code"))
            else -> error("payment gateway returned ${response.statusCode()}")
        }
    }

    private fun extract(json: String, field: String): String =
        Regex("\"$field\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1) ?: ""
}
