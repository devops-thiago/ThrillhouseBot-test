package school.invoicer

data class Config(
    val dbUrl: String,
    val currencies: List<String>,
    val overdueGraceDays: Int,
    val pageSize: Int,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Config = Config(
            dbUrl = env["INVOICER_DB_URL"] ?: "jdbc:postgresql://localhost/school",
            currencies = (env["INVOICER_CURRENCIES"] ?: "EUR").split(",").map { it.trim() },
            overdueGraceDays = env["INVOICER_OVERDUE_GRACE"]?.toInt() ?: 14,
            pageSize = env["INVOICER_PAGE_SIZE"]?.toInt() ?: 100,
        )
    }
}
