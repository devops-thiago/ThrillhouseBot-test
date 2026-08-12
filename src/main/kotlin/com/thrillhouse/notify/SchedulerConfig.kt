package com.thrillhouse.notify

/**
 * Runtime configuration loaded from environment variables. See
 * docs/CONFIG-KOTLIN.md for the full list of supported settings.
 */
data class SchedulerConfig(
    val databaseUrl: String,
    val deliveryApiBaseUrl: String,
    val channels: List<String>,
    val pollIntervalSeconds: Int,
    val maxRetryAttempts: Int
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): SchedulerConfig {
            return SchedulerConfig(
                databaseUrl = env["NOTIFY_DB_URL"] ?: error("NOTIFY_DB_URL is required"),
                deliveryApiBaseUrl = env["NOTIFY_DELIVERY_API_BASE_URL"]
                    ?: error("NOTIFY_DELIVERY_API_BASE_URL is required"),
                channels = (env["NOTIFY_CHANNELS"] ?: "email").split(","),
                pollIntervalSeconds = env["NOTIFY_POLL_INTERVAL_SECONDS"]?.toInt() ?: 30,
                maxRetryAttempts = env["NOTIFY_MAX_RETRY_ATTEMPTS"]?.toInt() ?: 3
            )
        }
    }
}
