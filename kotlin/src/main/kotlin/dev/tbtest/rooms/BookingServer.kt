package dev.tbtest.rooms

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BookingServer(
    private val config: Config,
    private val service: BookingService,
    private val rooms: List<Room>,
    private val search: RoomSearch?,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    fun start(port: Int): HttpServer {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/bookings") { reply(it) { handleBook(it) } }
        server.createContext("/rooms") { reply(it) { handleSearch(it) } }
        // Concurrent requests: eight worker threads share one BookingService.
        server.executor = Executors.newFixedThreadPool(8)
        scheduler.scheduleAtFixedRate({ service.expireHolds() }, 1, 1, TimeUnit.MINUTES)
        server.start()
        return server
    }

    private fun handleBook(ex: HttpExchange): Pair<Int, String> {
        val q = query(ex)
        val room = rooms.firstOrNull { it.id == q["room"] } ?: return 404 to "unknown room"
        if (room.floor !in config.allowedFloors) return 403 to "floor not bookable"
        val start = Instant.parse(q.getValue("start"))
        val end = Instant.parse(q.getValue("end"))
        return try {
            val booking = service.book(room.id, q.getValue("organizer"), start, end)
            201 to booking.id
        } catch (e: SlotTakenException) {
            409 to (e.message ?: "conflict")
        }
    }

    private fun handleSearch(ex: HttpExchange): Pair<Int, String> {
        val q = query(ex)
        val found = search?.search(q["name"] ?: "", q["building"] ?: "") ?: emptyList()
        return 200 to found.joinToString("\n") { "${it.id},${it.name}" }
    }

    private fun query(ex: HttpExchange): Map<String, String> =
        (ex.requestURI.rawQuery ?: "").split("&").filter { it.contains("=") }.associate {
            val (k, v) = it.split("=", limit = 2)
            k to URLDecoder.decode(v, "UTF-8")
        }

    private fun reply(ex: HttpExchange, block: () -> Pair<Int, String>) {
        val (status, body) = try {
            block()
        } catch (e: Exception) {
            400 to (e.message ?: "bad request")
        }
        val bytes = body.toByteArray()
        ex.sendResponseHeaders(status, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }
}
