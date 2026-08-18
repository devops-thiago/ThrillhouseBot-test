package com.thrillhouse.meterfold

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

private val log: Logger = Logger.getLogger("com.thrillhouse.meterfold")

fun main() {
    val settings = Settings.fromEnvironment()
    val connections: () -> Connection = { DriverManager.getConnection(settings.jdbcUrl) }
    val priceBook = PriceBook.load(File(settings.priceBookFile), settings.defaultUnitCents)
    log.info("price book covers ${priceBook.meters.size} meters: ${priceBook.meters.sorted()}")

    val repository = RollupRepository(connections)
    val engine = RollupEngine(UsageEventStore(connections), repository, priceBook)

    val server = HttpServer.create(InetSocketAddress(settings.httpPort), 0)
    server.createContext("/billing", BillingRoutes(engine, repository))
    server.createContext("/health") { exchange ->
        val body = "ok\n".toByteArray()
        exchange.sendResponseHeaders(200, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }
    server.executor = Executors.newFixedThreadPool(HTTP_THREADS)
    server.start()

    Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
        { recalculateOpenPeriods(engine, settings) },
        0,
        settings.recalculateIntervalMinutes,
        TimeUnit.MINUTES,
    )

    log.info(
        "meterfold listening on ${settings.httpPort}, " +
            "recalculating every ${settings.recalculateIntervalMinutes}m",
    )
}

private const val HTTP_THREADS = 8

private fun recalculateOpenPeriods(engine: RollupEngine, settings: Settings) {
    try {
        engine.recalculateOpen(settings.lateArrivalGraceHours).forEach { rollup ->
            log.info(
                "recalculated ${rollup.period.id}: ${rollup.lines.size} lines, " +
                    "${rollup.tenantCount} tenants, ${rollup.totalCents} cents",
            )
        }
    } catch (failure: Exception) {
        // The scheduler stops on an escaping exception, and a database blip
        // must not take the recalculation loop down until the next deploy.
        log.log(Level.WARNING, failure) { "scheduled recalculation failed" }
    }
}
