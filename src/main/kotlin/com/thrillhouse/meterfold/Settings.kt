package com.thrillhouse.meterfold

/** Runtime settings, all read from the environment at start-up. */
data class Settings(
    val jdbcUrl: String,
    val httpPort: Int,
    val recalculateIntervalMinutes: Long,
    val lateArrivalGraceHours: Long,
    val priceBookFile: String,
    val defaultUnitCents: Long,
) {
    init {
        require(recalculateIntervalMinutes > 0) { "METERFOLD_RECALCULATE_INTERVAL_MINUTES must be positive" }
        require(lateArrivalGraceHours >= 0) { "METERFOLD_LATE_ARRIVAL_GRACE_HOURS cannot be negative" }
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): Settings = Settings(
            jdbcUrl = env["METERFOLD_JDBC_URL"] ?: "jdbc:postgresql://localhost:5432/metering",
            httpPort = env["METERFOLD_HTTP_PORT"]?.toInt() ?: 8080,
            recalculateIntervalMinutes = env["METERFOLD_RECALCULATE_INTERVAL_MINUTES"]?.toLong() ?: 10L,
            lateArrivalGraceHours = env["METERFOLD_LATE_ARRIVAL_GRACE_HOURS"]?.toLong() ?: 72L,
            priceBookFile = env["METERFOLD_PRICE_BOOK"] ?: "/etc/meterfold/price-book.properties",
            defaultUnitCents = env["METERFOLD_DEFAULT_UNIT_CENTS"]?.toLong() ?: 0L,
        )
    }
}
