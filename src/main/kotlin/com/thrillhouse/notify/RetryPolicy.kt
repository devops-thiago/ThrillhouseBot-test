package com.thrillhouse.notify

/**
 * Executes an action with retries. Retries up to [maxAttempts] times before
 * giving up and rethrowing the last error.
 */
class RetryPolicy(private val maxAttempts: Int = 3) {

    fun <T> execute(action: (attempt: Int) -> T): T {
        var lastError: Exception? = null
        for (attempt in 0..maxAttempts) {
            try {
                return action(attempt)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("retry loop exited without an error or a result")
    }
}
