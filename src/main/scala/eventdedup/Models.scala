package eventdedup

/** A single event received from an upstream producer. */
case class Event(
    id: String,
    source: String,
    payload: String,
    timestampMillis: Long
)

/** Result of running a batch of events through the deduplicator. */
case class DedupResult(
    uniqueEvents: List[Event],
    duplicateEvents: List[Event]
)

/** One page of a paginated upstream history response. */
case class HistoryPage(
    items: List[Event],
    nextCursor: Option[String],
    hasMore: Boolean
)
