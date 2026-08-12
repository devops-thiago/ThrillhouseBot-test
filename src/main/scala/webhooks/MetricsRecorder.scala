package webhooks

/**
 * Records delivery outcomes to the metrics pipeline. Implementations may apply
 * backpressure: when the recording queue is full, `record` returns `false`
 * instead of blocking the caller, and the caller is expected to log that and
 * continue rather than treat it as a delivery failure.
 */
trait MetricsRecorder {
  def record(attempt: DeliveryAttempt): Boolean
}

/** Metrics recorder backed by a bounded in-memory queue drained by a background reporter. */
class QueuedMetricsRecorder(capacity: Int) extends MetricsRecorder {
  private val queue = new java.util.concurrent.ArrayBlockingQueue[DeliveryAttempt](capacity)

  override def record(attempt: DeliveryAttempt): Boolean = queue.offer(attempt)
}
