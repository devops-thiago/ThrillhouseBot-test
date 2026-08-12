package webhooks

import java.time.Instant

/** An event queued for delivery to a subscriber's endpoint. */
final case class DeliveryEvent(
    eventId: String,
    subscriberId: String,
    eventType: String,
    payload: String,
    createdAt: Instant
)

/** The outcome of a single delivery attempt against a subscriber endpoint. */
final case class DeliveryAttempt(
    event: DeliveryEvent,
    attemptNumber: Int,
    succeeded: Boolean,
    statusCode: Option[Int],
    attemptedAt: Instant
)
