package com.thrillhouse.meterfold

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeParseException
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LinePayload(
    val tenant: String,
    val meter: String,
    val quantity: String,
    val chargeableUnits: Long,
    val amountCents: Long,
    val eventCount: Long,
    val latestEventAt: String,
)

@Serializable
data class PeriodPayload(
    val period: String,
    val calculatedAt: String,
    val tenants: Int,
    val totalCents: Long,
    val lines: List<LinePayload>,
)

@Serializable
data class TenantPayload(
    val period: String,
    val tenant: String,
    val totalCents: Long,
    val lines: List<LinePayload>,
)

@Serializable
data class PeriodsPayload(val periods: List<String>)

@Serializable
data class ErrorPayload(val error: String)

// Quantities are decimals with up to six places and go out as strings: the
// finance importer reads them with a decimal parser, and a JSON number would
// hand it a double.
private fun RollupLine.toPayload() = LinePayload(
    tenant = tenantId,
    meter = meter,
    quantity = quantity.toPlainString(),
    chargeableUnits = chargeableUnits,
    amountCents = amountCents,
    eventCount = eventCount,
    latestEventAt = latestEventAt.toString(),
)

private fun PeriodRollup.toPayload() = PeriodPayload(
    period = period.id,
    calculatedAt = calculatedAt.toString(),
    tenants = tenantCount,
    totalCents = totalCents,
    lines = lines.map { it.toPayload() },
)

/**
 * The billing API. Reads are served from the stored rollups; only the
 * recalculate route touches the event table.
 */
class BillingRoutes(
    private val engine: RollupEngine,
    private val rollups: RollupRepository,
) : HttpHandler {

    private data class Response(val status: Int, val body: String)

    override fun handle(exchange: HttpExchange) {
        val response = try {
            dispatch(exchange.requestMethod, exchange.requestURI.path)
        } catch (invalid: DateTimeParseException) {
            Response(400, JSON.encodeToString(ErrorPayload("period must be formatted as YYYY-MM")))
        } catch (failure: Exception) {
            LOG.log(Level.WARNING, failure) { "${exchange.requestMethod} ${exchange.requestURI.path} failed" }
            Response(500, JSON.encodeToString(ErrorPayload("internal error")))
        }
        respond(exchange, response)
    }

    private fun dispatch(method: String, path: String): Response {
        val segments = path.split('/').filter(String::isNotEmpty)
        if (segments.size < 2 || segments[0] != "billing" || segments[1] != "periods") {
            return notFound(path)
        }

        if (method == "GET" && segments.size == 2) {
            return Response(200, JSON.encodeToString(PeriodsPayload(rollups.storedPeriods())))
        }
        if (segments.size < 3) {
            return notFound(path)
        }

        val period = BillingPeriod.parse(segments[2])

        return when {
            method == "POST" && segments.size == 4 && segments[3] == "recalculate" ->
                Response(200, JSON.encodeToString(engine.recalculate(period).toPayload()))

            method == "GET" && segments.size == 3 ->
                rollups.read(period)
                    ?.let { Response(200, JSON.encodeToString(it.toPayload())) }
                    ?: Response(404, JSON.encodeToString(ErrorPayload("period ${period.id} has not been calculated")))

            method == "GET" && segments.size == 5 && segments[3] == "tenants" -> {
                val lines = rollups.readTenant(period, segments[4])
                val payload = TenantPayload(
                    period = period.id,
                    tenant = segments[4],
                    totalCents = lines.sumOf(RollupLine::amountCents),
                    lines = lines.map { it.toPayload() },
                )
                Response(200, JSON.encodeToString(payload))
            }

            else -> notFound(path)
        }
    }

    private fun notFound(path: String) =
        Response(404, JSON.encodeToString(ErrorPayload("no route for $path")))

    private fun respond(exchange: HttpExchange, response: Response) {
        val bytes = response.body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(response.status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private companion object {
        val LOG: Logger = Logger.getLogger(BillingRoutes::class.java.name)
        val JSON = Json { encodeDefaults = true }
    }
}
