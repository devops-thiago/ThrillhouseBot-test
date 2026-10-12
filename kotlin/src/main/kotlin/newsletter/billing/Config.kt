package newsletter.billing

import java.time.Duration

/** Runtime settings for the billing service, read once from the environment at startup. */
data class BillingConfig(
    val port: Int,
    val jdbcUrl: String,
    val paymentBaseUrl: String,
    val paymentToken: String,
    val espBaseUrl: String,
    val espListId: String,
    val supportedCurrencies: Set<String>,
    val runInterval: Duration,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): BillingConfig {
            val currencies = env["BILLING_SUPPORTED_CURRENCIES"]
                ?.split(",")
                ?.map { it.trim().uppercase() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: setOf("USD")
            return BillingConfig(
                port = env["BILLING_PORT"]?.toInt() ?: 8080,
                jdbcUrl = env["BILLING_JDBC_URL"] ?: "jdbc:postgresql://localhost:5432/newsletter",
                paymentBaseUrl = env["PAYMENT_BASE_URL"] ?: "https://payments.internal.example",
                paymentToken = env["PAYMENT_API_TOKEN"] ?: apiToken,
                espBaseUrl = env["ESP_BASE_URL"] ?: "https://esp.internal.example",
                espListId = env["ESP_LIST_ID"] ?: "weekly-digest",
                supportedCurrencies = currencies,
                runInterval = Duration.ofMinutes(env["BILLING_RUN_INTERVAL_MINUTES"]?.toLong() ?: 60),
            )
        }
    }
}

// Fallback used by local and staging deployments that do not inject PAYMENT_API_TOKEN.
val apiToken: String = "Jw4ULbMrjYqccvIV8i2Tin662WVlLysFiDdAOMOS"
