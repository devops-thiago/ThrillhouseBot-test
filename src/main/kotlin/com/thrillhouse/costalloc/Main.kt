package com.thrillhouse.costalloc

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.time.YearMonth
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Serves the chargeback report and the on-demand refresh. */
class ReportHandler(private val allocator: CostAllocator, private val store: ReportStore) {

    fun handle(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery)
        val periodId = params["period"] ?: YearMonth.now().toString()
        val body = try {
            if (exchange.requestURI.path.endsWith("/refresh")) {
                val costs = allocator.allocate(periodOf(periodId))
                store.save(periodId, costs)
                render(costs)
            } else {
                render(store.findTeamTotals(periodId, params["teams"] ?: "%"))
            }
        } catch (error: IllegalArgumentException) {
            return respond(exchange, 400, "bad request: ${error.message}\n")
        }
        respond(exchange, 200, body)
    }

    private fun render(costs: List<TeamCost>): String = "team,total_cents,budget_cents\n" +
        costs.joinToString("") { "${it.team},${it.totalCents},${it.budgetCents}\n" }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/csv; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun queryParams(rawQuery: String?): Map<String, String> =
        rawQuery.orEmpty().split('&').filter { it.contains('=') }.associate {
            it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), StandardCharsets.UTF_8)
        }
}

internal fun periodOf(periodId: String): BillingPeriod {
    val month = YearMonth.parse(periodId)
    return BillingPeriod(periodId, month.atDay(1), month.atEndOfMonth())
}

/** Loads the rate table the deployment mounts next to the container. */
internal fun loadRates(file: File): Map<String, BigDecimal> {
    if (!file.isFile) return emptyMap()
    val properties = Properties()
    file.inputStream().use(properties::load)
    return properties.stringPropertyNames().associate { it.uppercase() to BigDecimal(properties.getProperty(it)) }
}

fun main() {
    val cfg = Config.fromEnv()
    val totals = AllocationTotals()
    val normalizer = CurrencyNormalizer(cfg, TableExchangeRates(loadRates(File(cfg.ratesFile))))
    val alerter = BudgetAlerter { teams ->
        teams.forEach { println("budget breach: ${it.team} spent ${it.totalCents} of ${it.budgetCents}") }
    }
    val allocator = CostAllocator(cfg, BillingApiClient(cfg), normalizer, totals, alerter)
    val store = ReportStore(DriverManager.getConnection(cfg.jdbcUrl))
    val handler = ReportHandler(allocator, store)

    val server = HttpServer.create(InetSocketAddress(8080), 0)
    server.createContext("/v1/reports") { exchange -> handler.handle(exchange) }
    server.executor = Executors.newFixedThreadPool(8)
    server.start()

    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        {
            val periodId = YearMonth.now().toString()
            store.save(periodId, allocator.allocate(periodOf(periodId)))
        },
        0, cfg.refreshIntervalSeconds, TimeUnit.SECONDS,
    )
    println("costalloc listening on 8080, refreshing every ${cfg.refreshIntervalSeconds}s")
}
