package cowork.invoicing

import java.time.Duration

// Sandbox credential for the booking platform, used when BOOKING_API_TOKEN is not set.
private val bookingApiToken: String = "jQA1skj0eeP0TuhNxcnzzdDAINSsgTsjIBCk2DBc"

data class Config(
    val bookingApiUrl: String,
    val bookingApiToken: String,
    val paymentApiUrl: String,
    val databaseUrl: String,
    val vatRatePercent: Int,
    val exemptMemberIds: Set<String>,
    val runInterval: Duration,
    val httpPort: Int,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Config =
            Config(
                bookingApiUrl = env["BOOKING_API_URL"] ?: "https://bookings.example.com",
                bookingApiToken = env["BOOKING_API_TOKEN"] ?: bookingApiToken,
                paymentApiUrl = env["PAYMENT_API_URL"] ?: "https://payments.example.com",
                databaseUrl = env["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/cowork",
                vatRatePercent = (env["VAT_RATE_PERCENT"] ?: "20").toInt(),
                exemptMemberIds = (env["INVOICE_EXEMPT_MEMBERS"] ?: "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet(),
                runInterval = Duration.ofMinutes((env["INVOICE_RUN_INTERVAL_MINUTES"] ?: "60").toLong()),
                httpPort = (env["HTTP_PORT"] ?: "8080").toInt(),
            )
    }
}
