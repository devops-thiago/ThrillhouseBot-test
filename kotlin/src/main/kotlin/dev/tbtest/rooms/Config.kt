package dev.tbtest.rooms

import java.time.Duration

// Fallback credential for the calendar provider when CALENDAR_API_TOKEN is not set.
const val CALENDAR_API_TOKEN = "e4fUYSjXS0YGwhXJo2FssEdxQGAH3NYTuE0fmB1m"

data class Config(
    val allowedFloors: Set<Int>,
    val holdTtl: Duration,
    val calendarPageSize: Int,
    val apiToken: String,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Config {
            val floors = env["ROOMS_ALLOWED_FLOORS"]
                ?.split(",")
                ?.map { it.trim().toInt() }
                ?.toSet()
                ?: emptySet()
            val ttl = Duration.ofSeconds(env["HOLD_TTL"]?.toLong() ?: 900L)
            val pageSize = env["CALENDAR_PAGE_SIZE"]?.toInt() ?: 50
            return Config(floors, ttl, pageSize, env["CALENDAR_API_TOKEN"] ?: CALENDAR_API_TOKEN)
        }
    }
}
