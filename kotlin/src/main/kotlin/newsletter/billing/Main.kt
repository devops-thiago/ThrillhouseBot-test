package newsletter.billing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.sql.DriverManager
import java.time.YearMonth
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val mapper = jacksonObjectMapper()

private val PLANS = listOf(
    Plan("digest-monthly", 900, Cadence.MONTHLY),
    Plan("digest-annual", 900, Cadence.ANNUAL),
    Plan("pro-monthly", 2500, Cadence.MONTHLY),
).associateBy { it.code }

fun main() {
    val config = BillingConfig.fromEnv()
    val repository = SubscriberRepository { DriverManager.getConnection(config.jdbcUrl) }
    val esp = EspClient(config.espBaseUrl)
    val service = BillingService(
        store = repository,
        gateway = HttpPaymentGateway(config.paymentBaseUrl, config.paymentToken),
        plans = PLANS,
        supportedCurrencies = config.supportedCurrencies,
        confirmedEmails = { esp.confirmedEmails(config.espListId) },
        alert = { message -> System.err.println("[billing-alert] $message") },
    )

    val scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleAtFixedRate(
        { runCatching { service.runBillingCycle(YearMonth.now()) }.onFailure { it.printStackTrace() } },
        0,
        config.runInterval.toMinutes(),
        TimeUnit.MINUTES,
    )

    val server = HttpServer.create(InetSocketAddress(config.port), 0)
    server.createContext("/billing/run") { exchange ->
        if (exchange.requestMethod != "POST") return@createContext exchange.reply(405, mapOf("error" to "method not allowed"))
        val period = exchange.queryParams()["period"]?.let(YearMonth::parse) ?: YearMonth.now()
        exchange.reply(200, service.runBillingCycle(period))
    }
    server.createContext("/subscribers") { exchange ->
        val params = exchange.queryParams()
        val plan = params["plan"] ?: return@createContext exchange.reply(400, mapOf("error" to "plan is required"))
        exchange.reply(200, repository.findByPlan(plan, params["status"] ?: "active"))
    }
    server.executor = Executors.newFixedThreadPool(4)
    server.start()
    println("newsletter billing listening on :${config.port}")
}

private fun HttpExchange.queryParams(): Map<String, String> =
    requestURI.rawQuery.orEmpty().split("&").filter { it.contains("=") }.associate { pair ->
        val (k, v) = pair.split("=", limit = 2)
        URLDecoder.decode(k, Charsets.UTF_8) to URLDecoder.decode(v, Charsets.UTF_8)
    }

private fun HttpExchange.reply(status: Int, body: Any) {
    val bytes = mapper.writeValueAsBytes(body)
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}
