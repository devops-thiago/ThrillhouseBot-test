package cowork.invoicing

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.YearMonth
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    val config = Config.fromEnv()
    val repository = InvoiceRepository(config.databaseUrl)
    val service = InvoiceService(
        bookings = BookingClient(config.bookingApiUrl, config.bookingApiToken),
        members = repository,
        payments = HttpPaymentGateway(config.paymentApiUrl),
        store = repository,
        notifier = { period, invoices -> println("finance: ${invoices.size} unpaid invoices for $period") },
        vatRatePercent = config.vatRatePercent,
        exemptMemberIds = config.exemptMemberIds,
    )

    // Monthly invoicing runs on a timer; it bills the previous calendar month.
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleAtFixedRate(
        { service.run(YearMonth.now().minusMonths(1)) },
        0, config.runInterval.toMinutes(), TimeUnit.MINUTES,
    )

    val server = HttpServer.create(InetSocketAddress(config.httpPort), 0)
    // Finance staff can trigger a run by hand, e.g. after fixing a member's payment method.
    server.createContext("/invoices/run") { ex ->
        if (ex.requestMethod != "POST") return@createContext respond(ex, 405, "method not allowed")
        val period = ex.query("period")?.let(YearMonth::parse) ?: YearMonth.now().minusMonths(1)
        val created = service.run(period)
        respond(ex, 200, """{"created":${created.size}}""")
    }
    server.createContext("/invoices") { ex ->
        val member = ex.query("member") ?: return@createContext respond(ex, 400, "member is required")
        val zone = ex.query("zone") ?: return@createContext respond(ex, 400, "zone is required")
        if (zone.isBlank() || zone.length > 64) return@createContext respond(ex, 400, "invalid zone")
        val rows = repository.search(member, zone)
        respond(ex, 200, rows.joinToString(",", "[", "]") { """{"number":${it.number},"total":${it.totalCents},"status":"${it.status}"}""" })
    }
    server.createContext("/healthz") { ex -> respond(ex, 200, "ok") }
    server.executor = Executors.newFixedThreadPool(8)
    server.start()
}

private fun HttpExchange.query(name: String): String? =
    requestURI.rawQuery?.split("&")
        ?.map { it.split("=", limit = 2) }
        ?.firstOrNull { it[0] == name }
        ?.getOrNull(1)
        ?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8) }

private fun respond(ex: HttpExchange, status: Int, body: String) {
    val bytes = body.toByteArray()
    ex.responseHeaders.add("Content-Type", "application/json")
    ex.sendResponseHeaders(status, bytes.size.toLong())
    ex.responseBody.use { it.write(bytes) }
}
