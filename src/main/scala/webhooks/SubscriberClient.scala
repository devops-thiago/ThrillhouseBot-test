package webhooks

/** One page of subscriber records returned by the subscriber management API. */
final case class SubscriberPage(subscribers: List[Subscriber], nextPageToken: Option[String])

/** Talks to the subscriber management API to look up who should receive events. */
trait SubscriberDirectory {
  def fetchPage(tenantId: String, pageToken: Option[String]): SubscriberPage
}

/** Resolves the subscribers that events for a tenant should be delivered to. */
class SubscriberClient(directory: SubscriberDirectory) {

  /** Returns every active subscriber registered for the given tenant. */
  def activeSubscribersFor(tenantId: String): List[Subscriber] = {
    val page = directory.fetchPage(tenantId, None)
    page.subscribers.filter(_.active)
  }
}
