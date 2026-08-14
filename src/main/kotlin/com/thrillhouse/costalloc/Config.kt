package com.thrillhouse.costalloc

/** Runtime settings, all sourced from the environment. */
data class Config(
    val billingApiBase: String,
    val teamTagKey: String,
    val includedAccounts: List<String>,
    val refreshIntervalSeconds: Long,
    val defaultBudgetCents: Long,
    val unallocatedTeam: String,
    val reportingCurrency: String,
    val jdbcUrl: String,
    val ratesFile: String,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Config = Config(
            billingApiBase = env["COSTALLOC_BILLING_API"] ?: "https://billing.internal/v1",
            teamTagKey = env["COSTALLOC_TEAM_TAG"] ?: "owner-team",
            includedAccounts = env["COSTALLOC_INCLUDED_ACCOUNTS"]
                ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
            refreshIntervalSeconds = env["COSTALLOC_REFRESH_INTERVAL_SECONDS"]?.toLong() ?: 900L,
            defaultBudgetCents = env["COSTALLOC_DEFAULT_BUDGET_CENTS"]?.toLong() ?: 500_000L,
            unallocatedTeam = env["COSTALLOC_UNALLOCATED_TEAM"] ?: "unallocated",
            reportingCurrency = env["COSTALLOC_REPORTING_CURRENCY"] ?: "USD",
            jdbcUrl = env["COSTALLOC_JDBC_URL"] ?: "jdbc:postgresql://localhost:5432/costalloc",
            ratesFile = env["COSTALLOC_RATES_FILE"] ?: "/etc/costalloc/rates.properties",
        )
    }
}
