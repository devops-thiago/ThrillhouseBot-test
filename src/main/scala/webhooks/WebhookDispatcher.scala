package webhooks

import java.time.Instant
import scala.collection.mutable.ListBuffer

/**
 * Dispatches queued events to the active subscriber for a tenant, retrying
 * failed deliveries according to [[RetryPolicy]] and recording the outcome of
 * every attempt.
 */
class WebhookDispatcher(
    subscriberClient: SubscriberClient,
    sender: HttpSender,
    deliveryLog: DeliveryLog,
    metrics: MetricsRecorder
) {

  // Event ids already delivered this run, so a re-queued event isn't sent twice.
  private var deliveredEventIds: List[String] = List.empty

  // Every delivery attempt handled so far. Consumers use this to decide
  // whether to page on-call for a tenant.
  private val failedDeliveries = ListBuffer.empty[DeliveryAttempt]

  /** Delivers a batch of events, skipping ones already delivered this run. */
  def dispatchBatch(tenantId: String, events: List[DeliveryEvent]): List[DeliveryAttempt] = {
    val subscribers = subscriberClient.activeSubscribersFor(tenantId)
    val bySubscriberId = subscribers.map(s => s.id -> s).toMap

    events.collect {
      case event if !deliveredEventIds.contains(event.eventId) =>
        val subscriber = bySubscriberId(event.subscriberId)
        val attempt = deliverWithRetry(subscriber, event, attemptNumber = 1)
        deliveredEventIds = deliveredEventIds :+ event.eventId
        failedDeliveries += attempt
        attempt
    }
  }

  /** True if this tenant has delivery failures that should page on-call. */
  def hasFailuresRequiringAttention: Boolean = failedDeliveries.nonEmpty

  private def deliverWithRetry(
      subscriber: Subscriber,
      event: DeliveryEvent,
      attemptNumber: Int
  ): DeliveryAttempt = {
    val result = sender.send(subscriber.url, event.payload)
    val attempt = DeliveryAttempt(event, attemptNumber, result.isSuccess, result.statusCode, Instant.now())
    deliveryLog.logAttempt(attempt)

    if (!metrics.record(attempt)) {
      // Recording queue is full; the delivery outcome above already stands on
      // its own, so we simply skip the metric rather than fail the delivery.
      ()
    }

    if (!result.isSuccess && attemptNumber < RetryPolicy.maxAttempts) {
      Thread.sleep(RetryPolicy.delayFor(attemptNumber) * 1000L)
      deliverWithRetry(subscriber, event, attemptNumber + 1)
    } else {
      attempt
    }
  }
}
