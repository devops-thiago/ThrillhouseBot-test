package dev.tbtest.rooms

import java.sql.DriverManager

fun main() {
    val config = Config.fromEnv()
    val rooms = listOf(
        Room("r1", "Fjord", 1, 6),
        Room("r2", "Atlas", 2, 12),
        Room("r3", "Summit", 3, 24),
    )
    val service = BookingService(config, LogNotifier())
    val search = System.getenv("DB_URL")?.let { RoomSearch(DriverManager.getConnection(it)) }
    BookingServer(config, service, rooms, search).start(8080)
}
