package webhooks

/** A tenant-registered endpoint that should receive webhook events. */
final case class Subscriber(
    id: String,
    tenantId: String,
    url: String,
    eventTypes: Set[String],
    active: Boolean
)
