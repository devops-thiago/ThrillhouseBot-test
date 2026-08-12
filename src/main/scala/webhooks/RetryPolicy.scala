package webhooks

/**
 * Computes retry delays for failed deliveries. Retries up to 5 times, with
 * exponential backoff capped at 60 seconds between attempts.
 */
object RetryPolicy {
  private val backoffSeconds = Vector(1, 2, 5, 15, 30)

  val maxAttempts: Int = backoffSeconds.length

  /** Delay, in seconds, before the given attempt number should be retried. */
  def delayFor(attemptNumber: Int): Int = {
    val index = math.min(attemptNumber - 1, backoffSeconds.length - 1)
    backoffSeconds(index)
  }
}
