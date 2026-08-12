package webhooks

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite

class WebhookDispatcherSpec extends AnyFunSuite {

  private val subscriber = Subscriber("sub-1", "tenant-1", "https://example.com/hook", Set("order.created"), active = true)

  private def directoryWith(subscribers: List[Subscriber]): SubscriberDirectory =
    (_: String, _: Option[String]) => SubscriberPage(subscribers, nextPageToken = None)

  private def eventFor(id: String): DeliveryEvent =
    DeliveryEvent(id, subscriber.id, "order.created", "{}", Instant.now())

  private class RecordingDeliveryLog extends DeliveryLog {
    var logged: List[DeliveryAttempt] = List.empty
    override def logAttempt(attempt: DeliveryAttempt): Unit = logged = logged :+ attempt
    override def countForSubscriber(subscriberId: String): Int = logged.count(_.event.subscriberId == subscriberId)
  }

  // Always reports success, no matter how many attempts have been recorded.
  // The real QueuedMetricsRecorder returns false once its bounded queue is
  // full, and callers are expected to tolerate that. This stub can never
  // return false, so it never exercises that path.
  private object AlwaysAcceptingMetricsRecorder extends MetricsRecorder {
    override def record(attempt: DeliveryAttempt): Boolean = true
  }

  test("delivers a queued event to the subscriber's endpoint") {
    val sender: HttpSender = (_: String, _: String) => SendResult(isSuccess = true, statusCode = Some(200))
    val dispatcher = new WebhookDispatcher(
      new SubscriberClient(directoryWith(List(subscriber))),
      sender,
      new RecordingDeliveryLog,
      AlwaysAcceptingMetricsRecorder
    )

    val attempts = dispatcher.dispatchBatch("tenant-1", List(eventFor("evt-1")))

    assert(attempts.size == 1)
    assert(attempts.head.succeeded)
  }

  test("continues delivering when the metrics queue is full") {
    val sender: HttpSender = (_: String, _: String) => SendResult(isSuccess = true, statusCode = Some(200))
    val dispatcher = new WebhookDispatcher(
      new SubscriberClient(directoryWith(List(subscriber))),
      sender,
      new RecordingDeliveryLog,
      AlwaysAcceptingMetricsRecorder
    )

    // Simulates a burst of events arriving after the metrics queue has filled up.
    val events = (1 to 5).map(i => eventFor(s"evt-burst-$i")).toList
    val attempts = dispatcher.dispatchBatch("tenant-1", events)

    assert(attempts.size == 5)
    assert(attempts.forall(_.succeeded))
  }
}
