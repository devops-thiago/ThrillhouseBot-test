package dlq

/** A single dead-letter queue entry as returned by the queue API. */
final case class Message(
    id: String,
    groupId: String,
    payload: Option[String],
    attempts: Int
)

/** One page of results from the queue's list endpoint. */
final case class MessagePage(items: Seq[Message], nextCursor: Option[String])
