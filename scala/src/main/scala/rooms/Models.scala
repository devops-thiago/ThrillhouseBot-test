package rooms

/** A bookable meeting room. */
final case class Room(id: String, name: String, capacity: Int, tags: Seq[String])

/** A reservation. `start` and `end` are epoch minutes; `end` is exclusive. */
final case class Booking(
    id: String,
    roomId: String,
    organiser: String,
    start: Long,
    end: Long,
    confirmed: Boolean = false
)

/** One page of a cursor-paginated response. `nextCursor` is None on the last page. */
final case class Page[A](items: Seq[A], nextCursor: Option[String])
